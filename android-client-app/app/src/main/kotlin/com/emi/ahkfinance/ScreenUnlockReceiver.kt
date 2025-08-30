package com.emi.ahkfinance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ScreenUnlockReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ScreenUnlockReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "User unlocked the device")
                handleScreenUnlock(context)
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned on")
                // Screen turned on, but may still be locked
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen turned off")
                // Screen turned off
            }
        }
    }
    
    private fun handleScreenUnlock(context: Context) {
        // Check if app should be locked using EncryptedSharedPreferences
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                "ahk_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val isAppLocked = prefs.getBoolean("isLocked", true)
            
            Log.d(TAG, "Checking lock status: isAppLocked = $isAppLocked")
            
            if (isAppLocked) {
                // Check if MainActivity is already running to avoid conflicts
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val runningTasks = try {
                    activityManager.getRunningTasks(1)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot get running tasks: ${e.message}")
                    emptyList()
                }
                
                val isMainActivityRunning = runningTasks.any { 
                    it.topActivity?.className == "com.emi.ahkfinance.MainActivity" 
                }
                
                if (!isMainActivityRunning) {
                    Log.d(TAG, "App is locked and MainActivity not running, showing lock screen")
                    
                    // Launch the lock screen activity
                    val lockIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                               Intent.FLAG_ACTIVITY_CLEAR_TOP or
                               Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("show_lock_screen", true)
                    }
                    context.startActivity(lockIntent)
                } else {
                    Log.d(TAG, "MainActivity already running, skipping lock screen launch")
                }
            } else {
                Log.d(TAG, "App is unlocked, not showing lock screen")
                
                // If app is unlocked and MainActivity is running with lock screen, close it
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val runningTasks = try {
                    activityManager.getRunningTasks(1)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot get running tasks: ${e.message}")
                    emptyList()
                }
                
                val isMainActivityRunning = runningTasks.any { 
                    it.topActivity?.className == "com.emi.ahkfinance.MainActivity" 
                }
                
                if (isMainActivityRunning) {
                    Log.d(TAG, "App is unlocked but MainActivity is running, sending close broadcast")
                    // Send broadcast to close lock screen
                    val closeIntent = Intent("com.emi.ahkfinance.CLOSE_LOCK_SCREEN")
                    context.sendBroadcast(closeIntent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking lock status with EncryptedSharedPreferences: ${e.message}")
            // Fallback to regular SharedPreferences
            val prefs = context.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
            val isAppLocked = prefs.getBoolean("isLocked", true)
            
            Log.d(TAG, "Fallback - Checking lock status: isAppLocked = $isAppLocked")
            
            if (isAppLocked) {
                Log.d(TAG, "App is locked (fallback), showing lock screen")
                
                // Launch the lock screen activity
                val lockIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP or
                           Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("show_lock_screen", true)
                }
                context.startActivity(lockIntent)
            } else {
                Log.d(TAG, "App is unlocked (fallback), not showing lock screen")
            }
        }
    }
}