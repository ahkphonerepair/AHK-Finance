package com.emi.ahkfinance

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class LocationSyncWorker(context: Context, params: WorkerParameters) : 
    CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "LocationSyncWorker"
        
        fun syncLocationData(context: Context) {
            // Use WorkManager to schedule sync instead of direct execution
            val syncRequest = OneTimeWorkRequestBuilder<LocationSyncWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            if (!isNetworkAvailable()) {
                Log.d(TAG, "No network available, skipping sync")
                return Result.retry()
            }
            
            syncUnsyncedData()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing location data", e)
            Result.retry()
        }
    }
    
    private suspend fun syncUnsyncedData() {
        val dbHelper = LocationDatabaseHelper.getInstance(applicationContext)
        val unsyncedLocations = dbHelper.getUnsyncedLocationData()
        
        if (unsyncedLocations.isEmpty()) {
            Log.d(TAG, "No unsynced location data to upload")
            return
        }
        
        Log.d(TAG, "Syncing ${unsyncedLocations.size} location records")
        
        val firestore = FirebaseFirestore.getInstance()
        val deviceId = Settings.Secure.getString(
            applicationContext.contentResolver, 
            Settings.Secure.ANDROID_ID
        )
        
        // Group locations by date for organized storage
        val locationsByDate = unsyncedLocations.groupBy { it.date }
        
        val syncedLocationIds = mutableListOf<Long>()
        
        for ((date, locationsForDate) in locationsByDate) {
            try {
                // Create or update document for this date
                val dateDocRef = firestore
                    .collection("device_locations")
                    .document(deviceId)
                    .collection("location_history")
                    .document(date)
                
                // Prepare location entries for this date
                val locationEntries = mutableMapOf<String, Any>()
                
                for (location in locationsForDate) {
                    val timeKey = location.time.replace(":", "_").replace(" ", "_")
                    locationEntries[timeKey] = mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "accuracy" to location.accuracy,
                        "timestamp" to location.timestamp,
                        "time" to location.time
                    )
                }
                
                // Add metadata
                locationEntries["date"] = date
                locationEntries["deviceId"] = deviceId
                locationEntries["lastUpdated"] = System.currentTimeMillis()
                locationEntries["totalEntries"] = locationsForDate.size
                
                // Upload to Firestore
                dateDocRef.set(locationEntries).await()
                
                // Mark these locations as synced
                val locationIds = locationsForDate.map { it.id }
                syncedLocationIds.addAll(locationIds)
                
                Log.d(TAG, "Successfully synced ${locationsForDate.size} locations for date: $date")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing locations for date $date", e)
                // Continue with other dates even if one fails
            }
        }
        
        // Mark successfully synced locations in local database
        if (syncedLocationIds.isNotEmpty()) {
            val markedCount = dbHelper.markMultipleLocationsAsSynced(syncedLocationIds)
            Log.d(TAG, "Marked $markedCount locations as synced in local database")
        }
        
        // Maintain 30-day rolling window after sync
        maintainLocationDataLimit(dbHelper)
    }
    
    private fun maintainLocationDataLimit(dbHelper: LocationDatabaseHelper) {
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
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}