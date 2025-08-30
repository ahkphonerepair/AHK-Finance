package com.emi.ahkfinance

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.SetOptions
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class LockService : Service() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var powerManager: PowerManager
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null
    
    companion object {
        private const val TAG = "LockService"
        private const val HEARTBEAT_INTERVAL = 5 * 60 * 1000L // 5 minutes
    }
    private var isLockScreenActive = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LockService created")
        
        // Initialize device policy manager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // Enable sequential lock mode by default
        val deviceAdminManager = DeviceAdminManager(this)
        if (deviceAdminManager.isDeviceAdminActive()) {
            deviceAdminManager.enableSequentialLockMode()
            Log.d(TAG, "Sequential lock mode enabled")
        }
        
        startForegroundService()
        scheduleDueDateWorker(this)
        
        // Listen for remote lock/unlock
        val deviceId = getDeviceId(this)
        FirebaseModule.listenForLockUnlock(deviceId,
            onLock = { 
                Log.d(TAG, "Remote lock command received")
                setLocked(true)
                lockDevice()
            },
            onUnlock = { 
                Log.d(TAG, "Remote unlock command received")
                setLocked(false)
                unlockDevice()
            }
        )
        
        // Start periodic heartbeat for last seen updates
        startPeriodicHeartbeat()
    }
    
    private fun startPeriodicHeartbeat() {
        heartbeatRunnable = object : Runnable {
            override fun run() {
                updateLastSeenTimestamp()
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL)
            }
        }
        heartbeatHandler.post(heartbeatRunnable!!)
        Log.d(TAG, "Periodic heartbeat started (every 5 minutes)")
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
                Log.d(TAG, "Heartbeat: Last seen timestamp updated: $currentTimestamp")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Heartbeat: Error updating last seen timestamp", e)
                // Retry with set() if document doesn't exist
                db.collection("devices").document(deviceId).set(
                    mapOf("lastSeenTimestamp" to currentTimestamp),
                    SetOptions.merge()
                )
                .addOnSuccessListener { 
                    Log.d(TAG, "Heartbeat: Last seen timestamp set successfully after update failed") 
                }
                .addOnFailureListener { retryError -> 
                    Log.e(TAG, "Heartbeat: Error setting last seen timestamp after retry", retryError) 
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat: Exception in updateLastSeenTimestamp: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "lock_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lock Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AHK Finance")
            .setContentText("Device Lock Monitoring Active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun setLocked(locked: Boolean) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                this,
                "ahk_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putBoolean("isLocked", locked).apply()
            Log.d(TAG, "Device lock status set to: $locked")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting lock status: ${e.message}")
            // Fallback to regular SharedPreferences
            val prefs = getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isLocked", locked).apply()
        }
    }

    private fun lockDevice() {
        try {
            Log.d(TAG, "Attempting to lock device")
            
            // Set kiosk mode in preferences for accessibility service
            setKioskMode(true)
            
            // Try device admin lock first (if available)
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                devicePolicyManager.lockNow()
                Log.d(TAG, "Device locked using DevicePolicyManager")
                
                // If device owner, use traditional kiosk mode
                if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                    startKioskMode()
                }
            } else {
                Log.w(TAG, "Device admin not active, using accessibility service for control")
            }
            
            // Show lock screen activity
            showLockScreen()
            
            // Check if accessibility service is enabled
            if (!isAccessibilityServiceEnabled()) {
                Log.w(TAG, "Accessibility service not enabled - device control may be limited")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error locking device: ${e.message}")
        }
    }
    
    private fun unlockDevice() {
        try {
            Log.d(TAG, "Attempting to unlock device")
            
            // Disable kiosk mode in preferences
            setKioskMode(false)
            
            // Reset lock screen active flag
            isLockScreenActive = false
            
            // Send broadcast to close lock screen activity
            val closeIntent = Intent("com.emi.ahkfinance.CLOSE_LOCK_SCREEN")
            sendBroadcast(closeIntent)
            Log.d(TAG, "Broadcast sent to close lock screen")
            
            // If device owner, stop traditional kiosk mode
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                stopKioskMode()
            }
            
            Log.d(TAG, "Device unlock completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking device: ${e.message}")
        }
    }
    
    private fun startKioskMode() {
        try {
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                Log.d(TAG, "Kiosk mode started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting kiosk mode: ${e.message}")
        }
    }
    
    private fun stopKioskMode() {
        try {
            // Kiosk mode will be stopped when the lock screen activity finishes
            Log.d(TAG, "Kiosk mode stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping kiosk mode: ${e.message}")
        }
    }

    private fun showLockScreen() {
        if (!isLockScreenActive) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("show_lock_screen", true)
            startActivity(intent)
            isLockScreenActive = true
            Log.d(TAG, "Lock screen activity started")
        } else {
            Log.d(TAG, "Lock screen already active, skipping")
        }
    }
    
    private fun setKioskMode(enabled: Boolean) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                this,
                "ahk_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putBoolean("kioskMode", enabled).apply()
            Log.d(TAG, "Kiosk mode set to: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting kiosk mode: ${e.message}")
            // Fallback to regular SharedPreferences
            val prefs = getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("kioskMode", enabled).apply()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val serviceName = "${packageName}/.AccessibilityControlService"
            return services?.contains(serviceName) == true
        }
        return false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop periodic heartbeat
        heartbeatRunnable?.let {
            heartbeatHandler.removeCallbacks(it)
            Log.d(TAG, "Periodic heartbeat stopped")
        }
        
        Log.d(TAG, "LockService destroyed")
    }
}

fun scheduleDueDateWorker(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<DueDateWorker>(15, TimeUnit.MINUTES)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "due_date_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}

class DueDateWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    companion object {
        private const val TAG = "DueDateWorker"
    }
    
    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "DueDateWorker executing...")
            
            // Use EncryptedSharedPreferences to match other parts of the app
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = try {
                EncryptedSharedPreferences.create(
                    applicationContext,
                    "ahk_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create EncryptedSharedPreferences, falling back to regular: ${e.message}")
                applicationContext.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
            }
            
            val dueDate = prefs.getString("dueDate", null)
            val isLocked = prefs.getBoolean("isLocked", false)
            val deviceId = getDeviceId(applicationContext)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            Log.d(TAG, "Due date: $dueDate, Today: $today, Is locked: $isLocked")
            
            if (dueDate != null && today >= dueDate && !isLocked) {
                Log.d(TAG, "Due date reached, locking device")
                
                // Update local state
                prefs.edit().putBoolean("isLocked", true).apply()
                
                // Update Firebase
                FirebaseModule.updateDeviceStatus(deviceId, mapOf("locked" to true))
                
                // Lock the device using DevicePolicyManager
                val devicePolicyManager = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminComponent = ComponentName(applicationContext, DeviceAdminReceiver::class.java)
                
                if (devicePolicyManager.isAdminActive(adminComponent)) {
                    devicePolicyManager.lockNow()
                    Log.d(TAG, "Device locked using DevicePolicyManager")
                }
                
                // Bring lock screen to foreground
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                applicationContext.startActivity(intent)
                
                Log.d(TAG, "Device locked due to due date")
            }
            
            return Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in DueDateWorker: ${e.message}")
            return Result.failure()
        }
    }
}