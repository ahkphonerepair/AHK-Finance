package com.emi.ahkfinance

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LocationTrackingService : Service(), LocationListener {
    
    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val LOCATION_UPDATE_INTERVAL = 60 * 60 * 1000L // 1 hour in milliseconds
        private const val MIN_DISTANCE_CHANGE = 10f // 10 meters
    }
    
    private lateinit var locationManager: LocationManager
    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var locationUpdateRunnable: Runnable? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationTrackingService created")
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        startLocationUpdates()
        
        // Schedule periodic location sync work
        scheduleLocationSyncWork()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LocationTrackingService started")
        
        // Try to start as foreground service, fall back to regular service if needed
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Started as foreground service")
        } catch (e: Exception) {
            Log.w(TAG, "Could not start as foreground service, continuing as regular service: ${e.message}")
            // Continue as regular service without notification
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationTrackingService destroyed")
        
        stopLocationUpdates()
        handler.removeCallbacks(locationUpdateRunnable ?: return)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks device location for security purposes"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AHK Finance - Location Tracking")
        .setContentText("Monitoring device location for security")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
    
    private fun startLocationUpdates() {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            return
        }
        
        try {
            // Request location updates from both GPS and Network providers
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    MIN_DISTANCE_CHANGE,
                    this
                )
                Log.d(TAG, "GPS location updates started")
            }
            
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    MIN_DISTANCE_CHANGE,
                    this
                )
                Log.d(TAG, "Network location updates started")
            }
            
            // Schedule hourly location capture
            scheduleHourlyLocationCapture()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
        }
    }
    
    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
            Log.d(TAG, "Location updates stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when stopping location updates", e)
        }
    }
    
    private fun scheduleHourlyLocationCapture() {
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                captureCurrentLocation()
                handler.postDelayed(this, LOCATION_UPDATE_INTERVAL)
            }
        }
        handler.post(locationUpdateRunnable!!)
    }
    
    private fun captureCurrentLocation() {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted for capture")
            return
        }
        
        try {
            val lastKnownGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            val bestLocation = when {
                lastKnownGPS != null && lastKnownNetwork != null -> {
                    if (lastKnownGPS.accuracy < lastKnownNetwork.accuracy) lastKnownGPS else lastKnownNetwork
                }
                lastKnownGPS != null -> lastKnownGPS
                lastKnownNetwork != null -> lastKnownNetwork
                else -> null
            }
            
            bestLocation?.let { location ->
                saveLocationData(location)
                Log.d(TAG, "Location captured: ${location.latitude}, ${location.longitude}")
            } ?: Log.w(TAG, "No location available for capture")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when capturing location", e)
        }
    }
    
    private fun saveLocationData(location: Location) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        
        val date = dateFormat.format(Date(currentTime))
        val time = timeFormat.format(Date(currentTime))
        
        val locationData = LocationData(
            deviceId = deviceId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = currentTime,
            date = date,
            time = time,
            synced = false
        )
        
        // Save to local database
        LocationDatabaseHelper.getInstance(this).insertLocationData(locationData)
        
        // Update last seen timestamp automatically
        updateLastSeenTimestamp()
        
        // Maintain 30-day rolling window
        maintainLocationDataLimit()
        
        // Try to sync immediately if online
        LocationSyncWorker.syncLocationData(this)
    }
    
    private fun maintainLocationDataLimit() {
        val dbHelper = LocationDatabaseHelper.getInstance(this)
        val distinctDatesCount = dbHelper.getDistinctDatesCount()
        
        // If we have more than 30 days of data, delete the oldest days
        if (distinctDatesCount > 30) {
            val daysToDelete = distinctDatesCount - 30
            Log.d(TAG, "Maintaining 30-day limit: deleting $daysToDelete oldest days")
            
            repeat(daysToDelete) {
                dbHelper.deleteOldestDayLocationData()
            }
        }
        
        Log.d(TAG, "Location data maintenance complete. Days stored: ${dbHelper.getDistinctDatesCount()}")
    }
    
    private fun updateLastSeenTimestamp() {
        try {
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val db = FirebaseFirestore.getInstance()
            val currentTimestamp = System.currentTimeMillis()
            
            db.collection("devices").document(deviceId).update(
                mapOf("lastSeenTimestamp" to currentTimestamp)
            )
            .addOnSuccessListener {
                Log.d(TAG, "Last seen timestamp updated automatically: $currentTimestamp")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating last seen timestamp", e)
                // Retry with set() if document doesn't exist
                db.collection("devices").document(deviceId).set(
                    mapOf("lastSeenTimestamp" to currentTimestamp),
                    SetOptions.merge()
                )
                .addOnSuccessListener { 
                    Log.d(TAG, "Last seen timestamp set successfully after update failed") 
                }
                .addOnFailureListener { retryError -> 
                    Log.e(TAG, "Error setting last seen timestamp after retry", retryError) 
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in updateLastSeenTimestamp: ${e.message}")
        }
    }
    
    private fun scheduleLocationSyncWork() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<LocationSyncWorker>(
            15, TimeUnit.MINUTES // Try to sync every 15 minutes
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "location_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
    
    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // LocationListener implementation
    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
        // Location updates are handled by scheduled captures
    }
    
    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Location provider enabled: $provider")
    }
    
    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Location provider disabled: $provider")
    }
    
    @Deprecated("Deprecated in API level 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(TAG, "Location provider status changed: $provider, status: $status")
    }
}

// Data class for location information
data class LocationData(
    val id: Long = 0,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val date: String,
    val time: String,
    val synced: Boolean = false
)