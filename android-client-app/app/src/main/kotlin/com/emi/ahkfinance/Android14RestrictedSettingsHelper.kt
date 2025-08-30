package com.emi.ahkfinance

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog

/**
 * Helper class for handling Android 14+ restricted settings for accessibility services
 */
class Android14RestrictedSettingsHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "Android14RestrictedHelper"
        
        /**
         * ADB command to grant WRITE_SECURE_SETTINGS permission
         */
        const val ADB_GRANT_COMMAND = "adb shell pm grant com.emi.ahkfinance android.permission.WRITE_SECURE_SETTINGS"
    }
    
    /**
     * Check if the device is running Android 14 or higher
     */
    fun isAndroid14OrHigher(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
    
    /**
     * Check if the app has WRITE_SECURE_SETTINGS permission
     */
    fun hasWriteSecureSettingsPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == 
                PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if the app is installed from a trusted source (Play Store)
     */
    fun isInstalledFromTrustedSource(): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            
            // Check if installed from Play Store or other trusted sources
            installer == "com.android.vending" || // Google Play Store
            installer == "com.google.android.packageinstaller" || // System installer
            installer == "com.android.packageinstaller" // System installer
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking installer source", e)
            false
        }
    }
    
    /**
     * Show comprehensive dialog for Android 14 restricted settings
     */
    fun showRestrictedSettingsDialog(
        onOpenAppSettings: () -> Unit,
        onOpenAccessibilitySettings: () -> Unit,
        onShowAdbInstructions: () -> Unit,
        onSkip: () -> Unit
    ) {
        val isTrustedSource = isInstalledFromTrustedSource()
        val hasWritePermission = hasWriteSecureSettingsPermission()
        
        val message = buildString {
            append("Android 14+ requires additional steps to enable accessibility services for apps not from the Play Store.\n\n")
            
            if (hasWritePermission) {
                append("✅ WRITE_SECURE_SETTINGS permission: Granted\n")
            } else {
                append("❌ WRITE_SECURE_SETTINGS permission: Not granted\n")
            }
            
            if (isTrustedSource) {
                append("✅ Trusted source: Yes\n\n")
            } else {
                append("❌ Trusted source: No (sideloaded APK)\n\n")
            }
            
            append("Choose an option below:")
        }
        
        AlertDialog.Builder(context)
            .setTitle("Accessibility Service Setup")
            .setMessage(message)
            .setPositiveButton("App Settings") { _, _ -> onOpenAppSettings() }
            .setNeutralButton("Accessibility") { _, _ -> onOpenAccessibilitySettings() }
            .setNegativeButton(if (hasWritePermission) "Skip" else "ADB Setup") { _, _ ->
                if (hasWritePermission) {
                    onSkip()
                } else {
                    onShowAdbInstructions()
                }
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show ADB instructions dialog for granting WRITE_SECURE_SETTINGS permission
     */
    fun showAdbInstructionsDialog(onDone: () -> Unit) {
        val instructions = """
            To grant WRITE_SECURE_SETTINGS permission via ADB:
            
            1. Enable Developer Options:
               • Go to Settings → About Phone
               • Tap "Build Number" 7 times
            
            2. Enable USB Debugging:
               • Go to Settings → Developer Options
               • Enable "USB Debugging"
            
            3. Connect device to computer and run:
               $ADB_GRANT_COMMAND
            
            4. Restart the app after running the command
            
            This allows the app to programmatically enable accessibility services without manual intervention.
        """.trimIndent()
        
        AlertDialog.Builder(context)
            .setTitle("ADB Setup Instructions")
            .setMessage(instructions)
            .setPositiveButton("Done") { _, _ -> onDone() }
            .setNegativeButton("Copy Command") { _, _ ->
                copyToClipboard(ADB_GRANT_COMMAND)
                onDone()
            }
            .show()
    }
    
    /**
     * Open app-specific settings where user can allow restricted settings
     */
    fun openAppSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
            false
        }
    }
    
    /**
     * Open accessibility settings
     */
    fun openAccessibilitySettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
            false
        }
    }
    
    /**
     * Copy text to clipboard
     */
    private fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("ADB Command", text)
            clipboard.setPrimaryClip(clip)
            
            android.widget.Toast.makeText(context, "Command copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to clipboard", e)
        }
    }
    
    /**
     * Get detailed status information for debugging
     */
    fun getStatusInfo(): String {
        return buildString {
            append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("Android 14+: ${isAndroid14OrHigher()}\n")
            append("WRITE_SECURE_SETTINGS: ${hasWriteSecureSettingsPermission()}\n")
            append("Trusted Source: ${isInstalledFromTrustedSource()}\n")
            append("Package: ${context.packageName}\n")
        }
    }
}