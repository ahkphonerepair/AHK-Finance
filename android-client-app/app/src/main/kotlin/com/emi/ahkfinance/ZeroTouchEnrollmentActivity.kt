package com.emi.ahkfinance

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
// import com.emi.ahkfinance.ui.theme.AHKFinanceTheme // Removed to avoid compilation issues
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity to handle Zero Touch enrollment and device provisioning
 * Processes provisioning intents and sets up device owner mode
 */
class ZeroTouchEnrollmentActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "ZeroTouchEnrollment"
        
        // Provisioning intent actions
        const val ACTION_PROVISION_MANAGED_DEVICE = "android.app.action.PROVISION_MANAGED_DEVICE"
        const val ACTION_PROVISION_MANAGED_PROFILE = "android.app.action.PROVISION_MANAGED_PROFILE"
        const val ACTION_GET_PROVISIONING_MODE = "android.app.action.GET_PROVISIONING_MODE"
        
        // Provisioning extras
        const val EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
        const val EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE = "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"
        const val EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"
    }
    
    private lateinit var deviceOwnerManager: DeviceOwnerManager
    private var enrollmentExtras: Bundle? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        deviceOwnerManager = DeviceOwnerManager(this)
        
        Log.d(TAG, "Zero Touch Enrollment Activity started")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent extras: ${intent.extras}")
        
        // Process provisioning intent
        processProvisioningIntent()
        
        setContent {
            ZeroTouchEnrollmentScreen(
                onEnrollmentComplete = { success ->
                    handleEnrollmentResult(success)
                },
                enrollmentExtras = enrollmentExtras
            )
        }
    }
    
    private fun processProvisioningIntent() {
        val action = intent.action
        val extras = intent.extras
        
        Log.d(TAG, "Processing provisioning intent with action: $action")
        
        when (action) {
            ACTION_PROVISION_MANAGED_DEVICE -> {
                Log.d(TAG, "Processing managed device provisioning")
                handleManagedDeviceProvisioning(extras)
            }
            ACTION_PROVISION_MANAGED_PROFILE -> {
                Log.d(TAG, "Processing managed profile provisioning")
                handleManagedProfileProvisioning(extras)
            }
            ACTION_GET_PROVISIONING_MODE -> {
                Log.d(TAG, "Getting provisioning mode")
                handleGetProvisioningMode()
            }
            else -> {
                Log.d(TAG, "Processing admin extras from intent")
                handleAdminExtras(extras)
            }
        }
    }
    
    private fun handleManagedDeviceProvisioning(extras: Bundle?) {
        if (extras == null) {
            Log.w(TAG, "No extras provided for managed device provisioning")
            return
        }
        
        val componentName = extras.getString(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME)
        val adminExtras = extras.getBundle(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        
        Log.d(TAG, "Device admin component: $componentName")
        Log.d(TAG, "Admin extras: $adminExtras")
        
        if (componentName == "${packageName}/.DeviceAdminReceiver") {
            enrollmentExtras = adminExtras
            
            // Process admin extras
            if (adminExtras != null) {
                deviceOwnerManager.handleZeroTouchExtras(adminExtras)
            }
            
            // Set result for provisioning
            val resultIntent = Intent()
            resultIntent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, adminExtras)
            setResult(Activity.RESULT_OK, resultIntent)
        } else {
            Log.w(TAG, "Component name mismatch: expected ${packageName}/.DeviceAdminReceiver, got $componentName")
            setResult(Activity.RESULT_CANCELED)
        }
    }
    
    private fun handleManagedProfileProvisioning(extras: Bundle?) {
        Log.d(TAG, "Managed profile provisioning not supported, switching to device owner mode")
        handleManagedDeviceProvisioning(extras)
    }
    
    private fun handleGetProvisioningMode() {
        val resultIntent = Intent()
        // Use the correct constant for managed device provisioning
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            resultIntent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, "managed_device")
        }
        setResult(Activity.RESULT_OK, resultIntent)
        Log.d(TAG, "Returned provisioning mode: MANAGED_DEVICE")
    }
    
    private fun handleAdminExtras(extras: Bundle?) {
        if (extras != null) {
            enrollmentExtras = extras
            deviceOwnerManager.handleZeroTouchExtras(extras)
        }
    }
    
    private fun handleEnrollmentResult(success: Boolean) {
        if (success) {
            Log.d(TAG, "Enrollment completed successfully")
            
            // Navigate to main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            Log.e(TAG, "Enrollment failed")
            setResult(Activity.RESULT_CANCELED)
        }
        
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroTouchEnrollmentScreen(
    onEnrollmentComplete: (Boolean) -> Unit,
    enrollmentExtras: Bundle?
) {
    val context = LocalContext.current
    val deviceOwnerManager = remember { DeviceOwnerManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var enrollmentStep by remember { mutableStateOf("Initializing...") }
    var isProcessing by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    var enrollmentDetails by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Process enrollment automatically
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val details = mutableListOf<String>()
                
                // Step 1: Check device owner status
                enrollmentStep = "Checking device owner status..."
                progress = 0.2f
                delay(1000)
                
                val isDeviceOwner = deviceOwnerManager.isDeviceOwner()
                val isDeviceAdmin = deviceOwnerManager.isDeviceAdminActive()
                
                details.add("Device Owner: ${if (isDeviceOwner) "✅ Yes" else "❌ No"}")
                details.add("Device Admin: ${if (isDeviceAdmin) "✅ Yes" else "❌ No"}")
                
                // Step 2: Process enrollment extras
                enrollmentStep = "Processing enrollment configuration..."
                progress = 0.4f
                delay(1000)
                
                if (enrollmentExtras != null) {
                    val setupMode = enrollmentExtras.getString("setup_mode", "unknown")
                    val orgName = enrollmentExtras.getString("organization_name", "Unknown")
                    val autoGrant = enrollmentExtras.getBoolean("auto_grant_permissions", false)
                    val kioskMode = enrollmentExtras.getBoolean("enable_kiosk_mode", false)
                    
                    details.add("Setup Mode: $setupMode")
                    details.add("Organization: $orgName")
                    details.add("Auto Grant Permissions: ${if (autoGrant) "✅ Yes" else "❌ No"}")
                    details.add("Kiosk Mode: ${if (kioskMode) "✅ Enabled" else "❌ Disabled"}")
                    
                    deviceOwnerManager.handleZeroTouchExtras(enrollmentExtras)
                } else {
                    details.add("No enrollment extras found")
                }
                
                // Step 3: Apply device owner features
                if (isDeviceOwner) {
                    enrollmentStep = "Applying device owner configuration..."
                    progress = 0.6f
                    delay(1000)
                    
                    // Apply security restrictions
                    val restrictionsApplied = deviceOwnerManager.applySecurityRestrictions()
                    details.add("Security Restrictions: ${if (restrictionsApplied) "✅ Applied" else "❌ Failed"}")
                    
                    // Enable kiosk mode if requested
                    val kioskEnabled = enrollmentExtras?.getBoolean("enable_kiosk_mode", false) ?: false
                    if (kioskEnabled) {
                        val kioskResult = deviceOwnerManager.enableKioskMode()
                        details.add("Kiosk Mode: ${if (kioskResult) "✅ Enabled" else "❌ Failed"}")
                    }
                    
                    // Set organization name
                    val orgName = enrollmentExtras?.getString("organization_name")
                    if (!orgName.isNullOrEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val orgResult = deviceOwnerManager.setOrganizationName(orgName)
                        details.add("Organization Name: ${if (orgResult) "✅ Set" else "❌ Failed"}")
                    }
                }
                
                // Step 4: Finalize enrollment
                enrollmentStep = "Finalizing enrollment..."
                progress = 0.8f
                delay(1000)
                
                // Save enrollment completion status
                val prefs = context.getSharedPreferences("ahk_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("zero_touch_enrollment_completed", true)
                    putLong("enrollment_completion_time", System.currentTimeMillis())
                    apply()
                }
                
                details.add("Enrollment Status: ✅ Completed")
                
                // Step 5: Complete
                enrollmentStep = "Enrollment completed successfully!"
                progress = 1.0f
                enrollmentDetails = details
                delay(2000)
                
                isProcessing = false
                onEnrollmentComplete(true)
                
            } catch (e: Exception) {
                Log.e("ZeroTouchEnrollment", "Enrollment failed", e)
                enrollmentStep = "Enrollment failed: ${e.message}"
                isProcessing = false
                onEnrollmentComplete(false)
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "AHK Finance",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Zero Touch Enrollment",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Progress Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Enrollment Progress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = enrollmentStep,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Details Section
            if (enrollmentDetails.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Enrollment Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        enrollmentDetails.forEach { detail ->
                            Text(
                                text = detail,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Action Button
            if (!isProcessing) {
                Button(
                    onClick = { onEnrollmentComplete(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text("Continue to App")
                }
            }
        }
    }
}