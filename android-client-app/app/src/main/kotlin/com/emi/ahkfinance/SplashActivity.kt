package com.emi.ahkfinance

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.app.KeyguardManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SplashActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "SplashActivity"
        private const val ACCESSIBILITY_REQUEST_CODE = 1001
        private const val PERMISSIONS_REQUEST_CODE = 1002
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 1003
        private const val DEVICE_ADMIN_REQUEST_CODE = 1004
        private const val NOTIFICATION_POLICY_REQUEST_CODE = 1005
        private const val SYSTEM_ALERT_WINDOW_REQUEST_CODE = 1006
        
        // Basic permissions (excluding location permissions which are handled separately)
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        // Location permissions handled separately in sequence
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private var isDeviceOwner = false
    private var deviceId: String = ""
    private var setRegistrationStatusCallback: ((Boolean) -> Unit)? = null

    private var permissionCheckComplete = false
    private var allPermissionsGranted = false
    private var foregroundLocationGranted = false
    
    // Sequential permission tracking
    private var permissionQueue = mutableListOf<String>()
    private var currentPermissionIndex = 0
    private var isRequestingPermissions = false
    
    // Accessibility permission tracking
    private var isWaitingForAccessibilityResult = false
    private var accessibilityRetryCount = 0
    private val maxAccessibilityRetries = 3
    
    // Dialog management variables
    private var currentDialog: AlertDialog? = null
    private var isShowingPermissionDialog = false
    
    // Skip permission tracking
    private fun isSystemAlertWindowSkipped(): Boolean {
        val sharedPref = getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("system_alert_window_skipped", false)
    }
    
    private fun isNotificationPolicySkipped(): Boolean {
        val sharedPref = getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("notification_policy_skipped", false)
    }
    
    private fun setSystemAlertWindowSkipped(skipped: Boolean) {
        val sharedPref = getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("system_alert_window_skipped", skipped)
            apply()
        }
    }
    
    private fun setNotificationPolicySkipped(skipped: Boolean) {
        val sharedPref = getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("notification_policy_skipped", skipped)
            apply()
        }
    }
    
    // Dialog management helper methods
    private fun dismissCurrentDialog() {
        try {
            currentDialog?.let { dialog ->
                if (dialog.isShowing) {
                    dialog.dismiss()
                    Log.d(TAG, "Dialog dismissed successfully")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing dialog", e)
        } finally {
            currentDialog = null
            isShowingPermissionDialog = false
        }
    }

    private fun showDialog(dialogBuilder: AlertDialog.Builder): AlertDialog? {
        return try {
            // Dismiss any existing dialog first
            dismissCurrentDialog()
            
            // Create and show new dialog
            val dialog = dialogBuilder.create()
            currentDialog = dialog
            isShowingPermissionDialog = true
            
            dialog.show()
            Log.d(TAG, "Dialog shown successfully")
            dialog
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog", e)
            isShowingPermissionDialog = false
            null
        }
    }

    override fun onPause() {
        super.onPause()
        // Don't dismiss dialogs in onPause as user might be going to settings
        Log.d(TAG, "SplashActivity onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissCurrentDialog()
        Log.d(TAG, "SplashActivity onDestroy")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize device policy manager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        
        // Get device ID (ANDROID_ID)
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        // Check if app is Device Owner
        isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        
        Log.d(TAG, "Device Owner status: $isDeviceOwner")
        Log.d(TAG, "Device ID: $deviceId")
        
        // Initialize anti-uninstall protection
        AntiUninstallManager.initializeAntiUninstall(this)
        
        setContent {
            MaterialTheme {
                SplashScreen()
            }
        }
        
        // Start the comprehensive permission checking process
        startPermissionFlow()
    }
    
    @Composable
    private fun SplashScreen() {
        var statusMessage by remember { mutableStateOf(
            if (isDeviceOwner) "Initializing Device Owner Mode..." else "Checking Permissions..."
        ) }
        var showRegistrationMessage by remember { mutableStateOf(false) }
        var isRegistered by remember { mutableStateOf<Boolean?>(null) }
        
        // Update status message based on current state
        LaunchedEffect(isRegistered) {
            when (isRegistered) {
                null -> statusMessage = if (isDeviceOwner) "Initializing Device Owner Mode..." else "Checking Permissions..."
                false -> {
                    statusMessage = "This device is not registered"
                    showRegistrationMessage = true
                    // Remove automatic navigation - user must click button
                }
                true -> {
                    statusMessage = "Device is already registered"
                    showRegistrationMessage = true
                    delay(3000)
                    navigateToStatus()
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E3A8A))
        ) {
            // Main content centered
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "AHK Finance",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "মোবাইল সেবা নিন সহজ কিস্তিতে",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (!showRegistrationMessage) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                // Show Register Device button when device is not registered
                if (isRegistered == false) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { navigateToRegistration() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E3A8A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Register Device",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Development credit at the bottom
            Text(
                text = "Development by AHK Phone Repair",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
        
        // Expose the registration status setter
        LaunchedEffect(Unit) {
            setRegistrationStatusCallback = { registered ->
                isRegistered = registered
            }
        }
    }
    
    private fun startPermissionFlow() {
        Log.d(TAG, "Starting comprehensive permission flow")
        checkAllPermissions()
    }
    
    private fun checkAllPermissions() {
        if (isRequestingPermissions) {
            Log.d(TAG, "Already requesting permissions, skipping")
            return
        }
        
        Log.d(TAG, "Starting sequential permission check")
        
        // Build permission queue in order: Basic -> Location -> Background Location
        permissionQueue.clear()
        currentPermissionIndex = 0
        
        // Add basic permissions that are not granted
        for (permission in REQUIRED_PERMISSIONS) {
            // Skip FOREGROUND_SERVICE_LOCATION on Android versions below API 34 (Android 14)
            if (permission == Manifest.permission.FOREGROUND_SERVICE_LOCATION && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Log.d(TAG, "Skipping FOREGROUND_SERVICE_LOCATION permission on Android ${Build.VERSION.SDK_INT} (requires API 34+)")
                continue
            }
            
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionQueue.add(permission)
                Log.d(TAG, "Added to queue: $permission")
            }
        }
        
        // Add location permissions that are not granted
        for (permission in LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionQueue.add(permission)
                Log.d(TAG, "Added to queue: $permission")
            }
        }
        
        // Check if we have any permissions to request
        if (permissionQueue.isEmpty()) {
            Log.d(TAG, "All basic and location permissions already granted")
            checkBackgroundLocationPermission()
        } else {
            Log.d(TAG, "Starting sequential permission requests. Queue size: ${permissionQueue.size}")
            requestNextPermission()
        }
    }
    
    private fun requestNextPermission() {
        if (currentPermissionIndex >= permissionQueue.size) {
            Log.d(TAG, "All queued permissions processed, checking background location")
            isRequestingPermissions = false
            checkBackgroundLocationPermission()
            return
        }
        
        val permission = permissionQueue[currentPermissionIndex]
        Log.d(TAG, "Requesting permission ${currentPermissionIndex + 1}/${permissionQueue.size}: $permission")
        
        isRequestingPermissions = true
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            PERMISSIONS_REQUEST_CODE
        )
    }
    
    private fun checkBackgroundLocationPermission() {
        // Check if foreground location permissions are granted
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        foregroundLocationGranted = fineLocationGranted && coarseLocationGranted
        
        Log.d(TAG, "Foreground location granted: $foregroundLocationGranted")
        
        // Check background location permission (only if foreground location is granted and Android 10+)
        if (foregroundLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Background location permission needed")
                requestBackgroundLocationPermission()
                return
            }
        }
        
        Log.d(TAG, "All location permissions granted, checking special permissions")
        checkSpecialPermissionsStatus()
    }
    

    
    private fun checkSpecialPermissions() {
        // System Alert Window permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.d(TAG, "System Alert Window permission needed")
            }
        }
        
        // Notification Policy Access (Do Not Disturb)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Log.d(TAG, "Notification Policy Access needed")
            }
        }
        
        // Device Admin permission
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        if (!devicePolicyManager.isAdminActive(componentName)) {
            Log.d(TAG, "Device Admin permission needed")
        }
    }
    

    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                handlePermissionResults(permissions, grantResults)
            }
            BACKGROUND_LOCATION_REQUEST_CODE -> {
                handleBackgroundLocationResult(permissions, grantResults)
            }
        }
    }
    
    private fun handlePermissionResults(permissions: Array<out String>, grantResults: IntArray) {
        if (permissions.isNotEmpty()) {
            val permission = permissions[0]
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted: $permission")
                // Move to next permission in queue
                currentPermissionIndex++
                requestNextPermission()
            } else {
                Log.w(TAG, "Permission denied: $permission")
                // Show dialog explaining why this permission is needed
                showSinglePermissionExplanationDialog(permission)
            }
        }
    }
    
    private fun showSinglePermissionExplanationDialog(permission: String) {
        val permissionName = getPermissionDisplayName(permission)
        
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app requires the following permission to function properly:\n\n" +
                    permissionName + 
                    "\n\nPlease grant this permission to continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                // Request the denied permission again (stay on current index)
                isRequestingPermissions = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSIONS_REQUEST_CODE
                )
            }
            .setNegativeButton("Skip") { _, _ ->
                Log.d(TAG, "Permission skipped: $permission")
                // Move to next permission in queue
                currentPermissionIndex++
                requestNextPermission()
            }
            .setCancelable(false)
            .show()
    }
    

    
    private fun requestBackgroundLocationPermission() {
        Log.d(TAG, "Requesting background location permission")
        
        // Show explanation dialog first for background location
        AlertDialog.Builder(this)
            .setTitle("Background Location Permission")
            .setMessage("This app needs access to your location even when the app is not in use. This allows the app to track your device location for security and monitoring purposes.\n\nPlease select 'Allow all the time' in the next screen.")
            .setPositiveButton("Continue") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        BACKGROUND_LOCATION_REQUEST_CODE
                    )
                }
            }
            .setNegativeButton("Skip") { _, _ ->
                Log.d(TAG, "Background location permission skipped")
                checkSpecialPermissionsStatus()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun handleBackgroundLocationResult(permissions: Array<out String>, grantResults: IntArray) {
        if (permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Background location permission granted")
                checkSpecialPermissionsStatus()
            } else {
                Log.w(TAG, "Background location permission denied")
                // Show explanation and option to retry or skip
                AlertDialog.Builder(this)
                    .setTitle("Background Location Permission")
                    .setMessage("Background location access was not granted. The app can still function but some features may be limited.\n\nWould you like to try again?")
                    .setPositiveButton("Try Again") { _, _ ->
                        requestBackgroundLocationPermission()
                    }
                    .setNegativeButton("Continue") { _, _ ->
                        Log.d(TAG, "Proceeding without background location permission")
                        checkSpecialPermissionsStatus()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
    
    private fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_PHONE_STATE -> "• Phone State Access (for device identification)"
            Manifest.permission.READ_PHONE_NUMBERS -> "• Phone Numbers (for device verification)"
            Manifest.permission.CALL_PHONE -> "• Phone Calls (for support contact)"
            Manifest.permission.POST_NOTIFICATIONS -> "• Notifications (for system alerts)"
            Manifest.permission.WAKE_LOCK -> "• Wake Lock (for background operation)"
            Manifest.permission.RECEIVE_BOOT_COMPLETED -> "• Boot Receiver (for auto-start)"
            Manifest.permission.FOREGROUND_SERVICE -> "• Foreground Service (for continuous monitoring)"
            Manifest.permission.FOREGROUND_SERVICE_LOCATION -> "• Foreground Location Service (for location tracking while app is active)"
            Manifest.permission.INTERNET -> "• Internet Access (for data synchronization)"
            Manifest.permission.ACCESS_NETWORK_STATE -> "• Network State (for connectivity check)"
            Manifest.permission.ACCESS_FINE_LOCATION -> "• Fine Location (for precise location tracking)"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "• Coarse Location (for approximate location tracking)"
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "• Background Location (for location tracking when app is not in use)"
            else -> "• $permission"
        }
    }
    
    private fun checkSpecialPermissionsStatus() {
        // Check permissions in sequence: System Alert Window -> Notification Policy -> Device Admin -> Accessibility
        
        // 1. Check System Alert Window first - Skip on Android Go or if skipped
        if (!isAndroidGoDevice(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this) && !isSystemAlertWindowSkipped()) {
            Log.d(TAG, "System Alert Window permission needed")
            requestSystemAlertWindowPermission()
            return
        }
        
        // 2. Check Notification Policy Access second - Skip on Android Go or if skipped
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!isAndroidGoDevice(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted && !isNotificationPolicySkipped()) {
            Log.d(TAG, "Notification Policy Access permission needed")
            requestNotificationPolicyAccess()
            return
        }
        
        // 3. Check Device Admin third
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        if (!devicePolicyManager.isAdminActive(componentName)) {
            Log.d(TAG, "Device Admin permission needed")
            requestDeviceAdminPermission()
            return
        }
        
        // 4. Finally check Accessibility (last permission)
        Log.d(TAG, "All special permissions granted or skipped, checking accessibility")
        checkAccessibilityPermissionFinal()
    }
    
    private fun checkAccessibilityPermissionFinal() {
        if (isDeviceOwner) {
            Log.d(TAG, "Device Owner mode - skipping accessibility permission")
            proceedToFinalStep()
        } else {
            Log.d(TAG, "Checking accessibility permission (final step)")
            if (isAccessibilityServiceEnabled()) {
                Log.d(TAG, "Accessibility service already enabled")
                proceedToFinalStep()
            } else {
                Log.d(TAG, "Accessibility service not enabled, requesting")
                requestAccessibilityPermission()
            }
        }
    }
    
    private fun proceedToFinalStep() {
        Log.d(TAG, "All permissions granted, proceeding to device registration")
        allPermissionsGranted = true
        permissionCheckComplete = true
        
        checkDeviceRegistration()
    }
    
    private fun requestSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val dialogBuilder = AlertDialog.Builder(this)
                .setTitle("System Alert Window Permission")
                .setMessage("This app needs permission to display over other apps for security features. This permission is required to continue.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivityForResult(intent, SYSTEM_ALERT_WINDOW_REQUEST_CODE)
                }
                .setNegativeButton("Skip") { _, _ ->
                    setSystemAlertWindowSkipped(true)
                    checkSpecialPermissionsStatus()
                }
                .setCancelable(false)
            
            showDialog(dialogBuilder)
        } else {
            checkSpecialPermissionsStatus()
        }
    }
    
    private fun requestNotificationPolicyAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val dialogBuilder = AlertDialog.Builder(this)
                .setTitle("Notification Policy Access")
                .setMessage("This app needs access to notification policy settings for security features. This permission is required to continue.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    startActivityForResult(intent, NOTIFICATION_POLICY_REQUEST_CODE)
                }
                .setNegativeButton("Skip") { _, _ ->
                    setNotificationPolicySkipped(true)
                    checkSpecialPermissionsStatus()
                }
                .setCancelable(false)
            
            showDialog(dialogBuilder)
        } else {
            checkSpecialPermissionsStatus()
        }
    }
    
    private fun requestDeviceAdminPermission() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Device Administrator Permission")
            .setMessage("This app needs device administrator privileges for security features. This permission is required to continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app requires device admin privileges for security and kiosk mode functionality.")
                }
                startActivityForResult(intent, DEVICE_ADMIN_REQUEST_CODE)
            }
            .setNegativeButton("Exit App") { _, _ ->
                finish()
            }
            .setCancelable(false)
        
        showDialog(dialogBuilder)
    }
    
    private fun proceedToAccessibilityCheck() {
        Log.d(TAG, "All permissions granted, proceeding to accessibility check")
        allPermissionsGranted = true
        permissionCheckComplete = true
        
        if (isDeviceOwner) {
            Log.d(TAG, "Device Owner mode - skipping accessibility permission")
            proceedToFinalStep()
        } else {
            Log.d(TAG, "Non-Device Owner mode - checking accessibility permission")
            checkAccessibilityPermission()
        }
    }
    
    private fun checkAccessibilityPermission() {
        if (isAccessibilityServiceEnabled()) {
            Log.d(TAG, "Accessibility service already enabled")
            proceedToFinalStep()
        } else {
            Log.d(TAG, "Requesting accessibility permission")
            requestAccessibilityPermission()
        }
    }
    
    private fun checkAccessibilityWithRetry() {
        if (isAccessibilityServiceEnabled()) {
            Log.d(TAG, "Accessibility service is now enabled")
            isWaitingForAccessibilityResult = false
            accessibilityRetryCount = 0
            proceedToFinalStep()
        } else {
            accessibilityRetryCount++
            Log.d(TAG, "Accessibility service still not enabled, retry attempt: $accessibilityRetryCount/$maxAccessibilityRetries")
            
            if (accessibilityRetryCount < maxAccessibilityRetries) {
                // Retry with increasing delay
                val retryDelay = accessibilityRetryCount * 2000L // 2s, 4s, 6s
                Log.d(TAG, "Retrying accessibility check in ${retryDelay}ms")
                lifecycleScope.launch {
                    delay(retryDelay)
                    checkAccessibilityWithRetry()
                }
            } else {
                Log.d(TAG, "Max retry attempts reached, showing dialog again")
                isWaitingForAccessibilityResult = false
                accessibilityRetryCount = 0
                showAccessibilityRequiredDialog()
            }
        }
    }
    
    private fun showAccessibilityRequiredDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Accessibility Permission Required")
            .setMessage("Accessibility permission is still not enabled. This permission is required for the app to function properly.")
            .setPositiveButton("Try Again") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("Exit App") { _, _ ->
                finish()
            }
            .setCancelable(false)
        
        showDialog(dialogBuilder)
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
            val expectedService = "$packageName/${AccessibilityControlService::class.java.name}"
            return services?.contains(expectedService) == true
        }
        return false
    }
    
    private fun requestAccessibilityPermission() {
        // Check if we're on Android 14+ and handle restricted settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleAndroid14RestrictedSettings()
        } else {
            openAccessibilitySettings()
        }
    }
    
    private fun handleAndroid14RestrictedSettings() {
        val helper = Android14RestrictedSettingsHelper(this)
        
        // Log current status for debugging
        Log.d(TAG, "Android 14 Status: ${helper.getStatusInfo()}")
        
        // First try to enable programmatically if we have WRITE_SECURE_SETTINGS permission
        if (tryEnableAccessibilityServiceProgrammatically()) {
            Log.d(TAG, "Accessibility service enabled programmatically")
            proceedToFinalStep()
            return
        }
        
        // Show dialog requiring accessibility permission
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Accessibility Permission Required")
            .setMessage("This app requires accessibility permission to function properly. This is the final permission needed.")
            .setPositiveButton("Grant Permission") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("Exit App") { _, _ ->
                finish()
            }
            .setCancelable(false)
        
        showDialog(dialogBuilder)
    }
    
    private fun tryEnableAccessibilityServiceProgrammatically(): Boolean {
        return try {
            // Check if we have WRITE_SECURE_SETTINGS permission
            val hasPermission = checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == 
                    PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.d(TAG, "WRITE_SECURE_SETTINGS permission not granted")
                return false
            }
            
            // Get current enabled services
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            
            val serviceName = "$packageName/${AccessibilityControlService::class.java.name}"
            
            // Check if our service is already in the list
            if (enabledServices.contains(serviceName)) {
                Log.d(TAG, "Service already enabled")
                return true
            }
            
            // Add our service to the list
            val newServices = if (enabledServices.isEmpty()) {
                serviceName
            } else {
                "$enabledServices:$serviceName"
            }
            
            // Enable accessibility and add our service
            Settings.Secure.putString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newServices
            )
            
            Settings.Secure.putString(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                "1"
            )
            
            Log.d(TAG, "Accessibility service enabled programmatically")
            true
            
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException: Cannot write to secure settings", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling accessibility service programmatically", e)
            false
        }
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            
            // Show toast with instructions
            Toast.makeText(
                this,
                "Tap 'More' (⋮) → 'Allow restricted settings' → Then enable accessibility",
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
            openAccessibilitySettings()
        }
    }
    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivityForResult(intent, ACCESSIBILITY_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
            // Show error dialog and exit
            val dialogBuilder = AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Unable to open accessibility settings. Please enable accessibility permission manually in device settings.")
                .setPositiveButton("Exit App") { _, _ ->
                    finish()
                }
                .setCancelable(false)
            
            showDialog(dialogBuilder)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            ACCESSIBILITY_REQUEST_CODE -> {
                Log.d(TAG, "Returned from accessibility settings")
                isWaitingForAccessibilityResult = true
                // Give more time for the service to be enabled and implement retry mechanism
                lifecycleScope.launch {
                    delay(3000) // Increased from 1000ms to 3000ms
                    checkAccessibilityWithRetry()
                }
            }
            SYSTEM_ALERT_WINDOW_REQUEST_CODE -> {
                Log.d(TAG, "Returned from System Alert Window settings")
                // Continue checking other special permissions
                checkSpecialPermissionsStatus()
            }
            NOTIFICATION_POLICY_REQUEST_CODE -> {
                Log.d(TAG, "Returned from Notification Policy settings")
                // Continue checking other special permissions
                checkSpecialPermissionsStatus()
            }
            DEVICE_ADMIN_REQUEST_CODE -> {
                Log.d(TAG, "Returned from Device Admin settings")
                // Continue checking other special permissions
                checkSpecialPermissionsStatus()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "SplashActivity onResume")
        
        // Don't interfere if we're already showing a permission dialog
        if (isShowingPermissionDialog) {
            Log.d(TAG, "Permission dialog is already showing, skipping onResume checks")
            return
        }
        
        // Don't interfere if we're already waiting for accessibility result
        if (isWaitingForAccessibilityResult) {
            Log.d(TAG, "Already waiting for accessibility result, skipping onResume checks")
            return
        }
        
        // If permission check is complete and all permissions are granted,
        // check if we're returning from accessibility settings
        if (permissionCheckComplete && allPermissionsGranted) {
            if (isAccessibilityServiceEnabled()) {
                Log.d(TAG, "Accessibility service is enabled on resume")
                proceedToFinalStep()
            } else {
                Log.d(TAG, "Accessibility service not enabled on resume, continuing permission flow")
                checkSpecialPermissionsStatus()
            }
        } else if (!permissionCheckComplete) {
            // If permission check is not complete, continue the permission flow
            Log.d(TAG, "Continuing permission flow on resume")
            checkSpecialPermissionsStatus()
        }
    }
    
    private fun checkDeviceRegistration() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Checking device registration in Firebase")
                
                val firestore = FirebaseFirestore.getInstance()
                val deviceDoc = firestore.collection("devices").document(deviceId).get().await()
                
                if (deviceDoc.exists()) {
                    Log.d(TAG, "Device is registered")
                    
                    // Save registration data to SharedPreferences
                    saveRegistrationData(deviceDoc.data!!)
                    
                    // Update UI to show registration status
                    setRegistrationStatusCallback?.invoke(true)
                    
                    // Handle navigation based on device owner status and lock state
                    if (isDeviceOwner) {
                        // Device Owner mode - go directly to StatusActivity
                        // (Navigation will be handled by the UI callback after 3 seconds)
                    } else {
                        // Non-Device Owner mode - check lock status
                        val isLocked = deviceDoc.getBoolean("isLocked") ?: false
                        
                        if (isLocked) {
                            Log.d(TAG, "Device is locked - will launch LockScreen after message")
                            // Override the callback to navigate to lock screen instead
                            setRegistrationStatusCallback = { _ ->
                                lifecycleScope.launch {
                                    delay(3000)
                                    navigateToLockScreen()
                                }
                            }
                            setRegistrationStatusCallback?.invoke(true)
                        } else {
                            Log.d(TAG, "Device is unlocked - will launch StatusActivity after message")
                            // Override the callback to navigate to status screen
                            setRegistrationStatusCallback = { _ ->
                                lifecycleScope.launch {
                                    delay(3000)
                                    navigateToStatus()
                                }
                            }
                            setRegistrationStatusCallback?.invoke(true)
                        }
                    }
                } else {
                    Log.d(TAG, "Device is not registered")
                    
                    // Clear registration flag from SharedPreferences
                    clearRegistrationData()
                    
                    // Update UI to show not registered status
                    setRegistrationStatusCallback?.invoke(false)
                    // Navigation will be handled by the UI callback after 3 seconds
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking device registration", e)
                
                // Clear registration flag from SharedPreferences on error
                clearRegistrationData()
                
                // On error, assume device is not registered
                setRegistrationStatusCallback?.invoke(false)
            }
        }
    }
    
    private fun saveRegistrationData(data: Map<String, Any>) {
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
            
            val editor = prefs.edit()
            
            // Save all registration data
            data.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    else -> editor.putString(key, value.toString())
                }
            }
            
            // Set registration flag to indicate successful registration
            editor.putBoolean("isRegistered", true)
            
            editor.apply()
            Log.d(TAG, "Registration data saved to SharedPreferences")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving registration data", e)
        }
    }
    
    private fun clearRegistrationData() {
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
            
            val editor = prefs.edit()
            editor.putBoolean("isRegistered", false)
            editor.apply()
            
            Log.d(TAG, "Registration flag cleared from SharedPreferences")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing registration data", e)
        }
    }
    
    private fun navigateToLockScreen() {
        // Start location tracking service now that all permissions are granted
        try {
            val locationServiceIntent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Check if we have FOREGROUND_SERVICE_LOCATION permission for Android 14+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startForegroundService(locationServiceIntent)
                        Log.d(TAG, "LocationTrackingService started as foreground service")
                    } else {
                        Log.w(TAG, "FOREGROUND_SERVICE_LOCATION permission not granted, starting as regular service")
                        startService(locationServiceIntent)
                    }
                } else {
                    startForegroundService(locationServiceIntent)
                    Log.d(TAG, "LocationTrackingService started as foreground service")
                }
            } else {
                startService(locationServiceIntent)
                Log.d(TAG, "LocationTrackingService started as regular service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting LocationTrackingService", e)
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("show_lock_screen", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Set fullscreen immersive mode for lock screen
        // Modern approach for fullscreen and immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        // Modern approach for showing over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        startActivity(intent)
        finish()
    }
    
    private fun navigateToRegistration() {
        // Start location tracking service now that all permissions are granted
        try {
            val locationServiceIntent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Check if we have FOREGROUND_SERVICE_LOCATION permission for Android 14+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startForegroundService(locationServiceIntent)
                        Log.d(TAG, "LocationTrackingService started as foreground service")
                    } else {
                        Log.w(TAG, "FOREGROUND_SERVICE_LOCATION permission not granted, starting as regular service")
                        startService(locationServiceIntent)
                    }
                } else {
                    startForegroundService(locationServiceIntent)
                    Log.d(TAG, "LocationTrackingService started as foreground service")
                }
            } else {
                startService(locationServiceIntent)
                Log.d(TAG, "LocationTrackingService started as regular service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting LocationTrackingService", e)
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("start_destination", "registration")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    private fun navigateToStatus() {
        // Start location tracking service now that all permissions are granted
        try {
            val locationServiceIntent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Check if we have FOREGROUND_SERVICE_LOCATION permission for Android 14+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startForegroundService(locationServiceIntent)
                        Log.d(TAG, "LocationTrackingService started as foreground service")
                    } else {
                        Log.w(TAG, "FOREGROUND_SERVICE_LOCATION permission not granted, starting as regular service")
                        startService(locationServiceIntent)
                    }
                } else {
                    startForegroundService(locationServiceIntent)
                    Log.d(TAG, "LocationTrackingService started as foreground service")
                }
            } else {
                startService(locationServiceIntent)
                Log.d(TAG, "LocationTrackingService started as regular service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting LocationTrackingService", e)
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("start_destination", "status")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}