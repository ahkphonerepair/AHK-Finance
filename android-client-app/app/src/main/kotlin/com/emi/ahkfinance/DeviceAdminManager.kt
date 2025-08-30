package com.emi.ahkfinance

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class DeviceAdminManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceAdminManager"
        private const val REQUEST_CODE_ENABLE_ADMIN = 1001
    }
    
    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val componentName: ComponentName by lazy {
        ComponentName(context, DeviceAdminReceiver::class.java)
    }
    
    /**
     * Check if device admin is active
     */
    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(componentName)
    }
    
    /**
     * Request device admin privileges
     */
    fun requestDeviceAdminPrivileges(): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "AHK Finance needs device admin privileges to manage screen lock and security features."
        )
        return intent
    }
    
    /**
     * Disable native Android screen lock (for legacy mode or temporary disable)
     */
    fun disableNativeScreenLock(): Boolean {
        return try {
            if (isDeviceAdminActive()) {
                // Disable keyguard (native screen lock)
                devicePolicyManager.setKeyguardDisabled(componentName, true)
                
                // Only remove password requirements if not in sequential lock mode
                val prefs = context.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
                val isSequentialMode = prefs.getBoolean("sequential_lock_mode", false)
                
                if (!isSequentialMode) {
                    // Remove password requirements for legacy mode
                    devicePolicyManager.setPasswordQuality(
                        componentName, 
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED
                    )
                    devicePolicyManager.setPasswordMinimumLength(componentName, 0)
                }
                
                // Save state in preferences
                prefs.edit().putBoolean("native_lock_disabled", true).apply()
                
                Log.d(TAG, "Native screen lock disabled successfully (sequential mode: $isSequentialMode)")
                true
            } else {
                Log.w(TAG, "Device admin not active, cannot disable native screen lock")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable native screen lock", e)
            false
        }
    }
    
    /**
     * Enable sequential lock mode (native lock first, then app lock)
     */
    fun enableSequentialLockMode(): Boolean {
        return try {
            if (isDeviceAdminActive()) {
                // Keep native screen lock enabled
                devicePolicyManager.setKeyguardDisabled(componentName, false)
                
                // Set a basic password requirement to ensure native lock is active
                devicePolicyManager.setPasswordQuality(
                    componentName, 
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                )
                
                // Save state in preferences
                val prefs = context.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("sequential_lock_mode", true).apply()
                prefs.edit().putBoolean("native_lock_disabled", false).apply()
                
                Log.d(TAG, "Sequential lock mode enabled successfully")
                true
            } else {
                Log.w(TAG, "Device admin not active, cannot enable sequential lock mode")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable sequential lock mode", e)
            false
        }
    }
    
    /**
     * Enable native Android screen lock
     */
    fun enableNativeScreenLock(): Boolean {
        return try {
            if (isDeviceAdminActive()) {
                // Re-enable keyguard (native screen lock)
                devicePolicyManager.setKeyguardDisabled(componentName, false)
                
                // Save state in preferences
                val prefs = context.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("native_lock_disabled", false).apply()
                
                Log.d(TAG, "Native screen lock re-enabled successfully")
                true
            } else {
                Log.w(TAG, "Device admin not active, cannot enable native screen lock")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-enable native screen lock", e)
            false
        }
    }
    
    /**
     * Check if native screen lock is currently disabled
     */
    fun isNativeScreenLockDisabled(): Boolean {
        val prefs = context.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("native_lock_disabled", false)
    }
    
    /**
     * Check if sequential lock mode is enabled
     */
    fun isSequentialLockModeEnabled(): Boolean {
        val prefs = context.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("sequential_lock_mode", false)
    }
    
    /**
     * Force lock the device (if needed)
     */
    fun lockDevice(): Boolean {
        return try {
            if (isDeviceAdminActive()) {
                devicePolicyManager.lockNow()
                Log.d(TAG, "Device locked successfully")
                true
            } else {
                Log.w(TAG, "Device admin not active, cannot lock device")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock device", e)
            false
        }
    }
}