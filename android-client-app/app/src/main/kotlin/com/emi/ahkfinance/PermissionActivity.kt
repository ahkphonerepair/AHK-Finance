package com.emi.ahkfinance

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.emi.ahkfinance.ui.theme.AhkfinanceTheme

class PermissionActivity : ComponentActivity() {
    
    // Permission queue for sequential requests
    private var permissionQueue = mutableListOf<String>()
    private var currentPermissionIndex = 0
    private var isRequestingPermissions = false
    
    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkAllPermissions()
    }
    
    // Changed to single permission launcher for sequential requests
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handleSinglePermissionResult(isGranted)
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkAllPermissions()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AhkfinanceTheme {
                PermissionScreen(
                    onPermissionRequest = { requestPermissions() },
                    onContinue = { navigateToRegistration() },
                    checkPermissions = { checkAllPermissions() },
                    onRequestDeviceAdmin = { requestDeviceAdmin() },
                    onRequestOverlayPermission = { requestOverlayPermission() }
                )
            }
        }
    }
    
    private fun requestPermissions() {
        // Build permission queue for sequential requests
        permissionQueue.clear()
        currentPermissionIndex = 0
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionQueue.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        permissionQueue.add(Manifest.permission.READ_PHONE_STATE)
        permissionQueue.add(Manifest.permission.WAKE_LOCK)
        permissionQueue.add(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        permissionQueue.add(Manifest.permission.FOREGROUND_SERVICE)
        
        if (Build.VERSION.SDK_INT >= 34) {
            permissionQueue.add(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
        }
        
        // Start sequential permission requests
        requestNextPermission()
    }
    
    private fun requestNextPermission() {
        if (currentPermissionIndex >= permissionQueue.size) {
            // All permissions processed, update UI
            isRequestingPermissions = false
            checkAllPermissions()
            return
        }
        
        val permission = permissionQueue[currentPermissionIndex]
        
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, move to next
            currentPermissionIndex++
            requestNextPermission()
            return
        }
        
        // Request the permission
        isRequestingPermissions = true
        android.util.Log.d("PermissionActivity", "Requesting permission: $permission")
        permissionLauncher.launch(permission)
    }
    
    private fun handleSinglePermissionResult(isGranted: Boolean) {
        val permission = if (currentPermissionIndex < permissionQueue.size) {
            permissionQueue[currentPermissionIndex]
        } else {
            "unknown"
        }
        
        android.util.Log.d("PermissionActivity", "Permission result for $permission: $isGranted")
        
        // Move to next permission regardless of result
        currentPermissionIndex++
        
        // Continue with next permission
        requestNextPermission()
    }
    
    internal fun requestDeviceAdmin() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
            
            android.util.Log.d("PermissionActivity", "Requesting device admin permission...")
            
            if (!dpm.isAdminActive(adminComponent)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                    "ডিভাইস লক ও সুরক্ষার জন্য অ্যাডমিন অনুমতি প্রয়োজন। এটি ডিভাইস নিয়ন্ত্রণ ও নিরাপত্তার জন্য অত্যাবশ্যক।")
                
                // Add flags to ensure proper intent handling
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                
                try {
                    android.util.Log.d("PermissionActivity", "Launching device admin intent...")
                    deviceAdminLauncher.launch(intent)
                } catch (e: Exception) {
                    android.util.Log.e("PermissionActivity", "Failed to start device admin intent: ${e.message}", e)
                    // Try alternative method
                    try {
                        startActivity(intent)
                    } catch (e2: Exception) {
                        android.util.Log.e("PermissionActivity", "Alternative device admin intent also failed: ${e2.message}", e2)
                    }
                }
            } else {
                android.util.Log.d("PermissionActivity", "Device admin already active")
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionActivity", "Error requesting device admin: ${e.message}", e)
        }
    }
    
    internal fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            overlayPermissionLauncher.launch(intent)
        }
    }
    
    private fun checkAllPermissions(): PermissionStatus {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        val basicPermissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.FOREGROUND_SERVICE
        ).all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
        
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        
        val specialServicePermission = if (Build.VERSION.SDK_INT >= 34) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE) == PackageManager.PERMISSION_GRANTED
        } else true
        
        val deviceAdmin = dpm.isAdminActive(adminComponent)
        val isDeviceOwner = dpm.isDeviceOwnerApp(packageName)
        
        val overlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isAndroidGoDevice(this)) {
            Settings.canDrawOverlays(this)
        } else true
        
        android.util.Log.d("PermissionActivity", "Permission Status - Basic: $basicPermissions, Notification: $notificationPermission, Service: $specialServicePermission, DeviceAdmin: $deviceAdmin, DeviceOwner: $isDeviceOwner, Overlay: $overlayPermission")
        
        return PermissionStatus(
            basicPermissions = basicPermissions,
            notificationPermission = notificationPermission,
            specialServicePermission = specialServicePermission,
            deviceAdmin = deviceAdmin,
            overlayPermission = overlayPermission,
            isDeviceOwner = isDeviceOwner
        )
    }
    
    private fun navigateToRegistration() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("start_destination", "registration")
        startActivity(intent)
        finish()
    }
}

data class PermissionStatus(
    val basicPermissions: Boolean = false,
    val notificationPermission: Boolean = false,
    val specialServicePermission: Boolean = false,
    val deviceAdmin: Boolean = false,
    val overlayPermission: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val notificationSkipped: Boolean = false,
    val overlaySkipped: Boolean = false
) {
    val allGranted: Boolean
        get() = basicPermissions && (notificationPermission || notificationSkipped) && specialServicePermission && deviceAdmin && (overlayPermission || overlaySkipped)
    
    val hasMinimumPermissions: Boolean
        get() = basicPermissions && (notificationPermission || notificationSkipped) && specialServicePermission && deviceAdmin
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionRequest: () -> Unit,
    onContinue: () -> Unit,
    checkPermissions: () -> PermissionStatus,
    onRequestDeviceAdmin: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    var permissionStatus by remember { mutableStateOf(checkPermissions()) }
    var notificationSkipped by remember { mutableStateOf(false) }
    var overlaySkipped by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        permissionStatus = checkPermissions().copy(
            notificationSkipped = notificationSkipped,
            overlaySkipped = overlaySkipped
        )
    }
    
    LaunchedEffect(notificationSkipped, overlaySkipped) {
        permissionStatus = permissionStatus.copy(
            notificationSkipped = notificationSkipped,
            overlaySkipped = overlaySkipped
        )
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Device Permission Setup",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set up required permissions before registration",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Permission List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Permission Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    PermissionItem(
                        title = "Basic Permissions",
                        description = "Phone status, wake lock, boot receiver",
                        isGranted = permissionStatus.basicPermissions,
                        onClick = { onPermissionRequest() }
                    )
                    
                    PermissionItemWithSkip(
                        title = "Notification Permission",
                        description = "For displaying important messages",
                        isGranted = permissionStatus.notificationPermission,
                        isSkipped = permissionStatus.notificationSkipped,
                        onClick = { onPermissionRequest() },
                        onSkip = { notificationSkipped = true }
                    )
                    
                    PermissionItem(
                        title = "Foreground Service",
                        description = "Background monitoring service",
                        isGranted = permissionStatus.specialServicePermission,
                        onClick = { onPermissionRequest() }
                    )
                    
                    PermissionItem(
                        title = "Device Admin",
                        description = if (permissionStatus.isDeviceOwner) 
                            "Device admin active (Device Owner)" 
                        else "For device lock and control",
                        isGranted = permissionStatus.deviceAdmin,
                        onClick = onRequestDeviceAdmin,
                        isDeviceOwner = permissionStatus.isDeviceOwner
                    )
                    
                    PermissionItemWithSkip(
                        title = "System Alert Window Permission",
                        description = "Display over other apps",
                        isGranted = permissionStatus.overlayPermission,
                        isSkipped = permissionStatus.overlaySkipped,
                        onClick = onRequestOverlayPermission,
                        onSkip = { overlaySkipped = true }
                    )
                    
                    // Device Owner Status Card
                    if (permissionStatus.deviceAdmin) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (permissionStatus.isDeviceOwner) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (permissionStatus.isDeviceOwner) Icons.Default.Check else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (permissionStatus.isDeviceOwner) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (permissionStatus.isDeviceOwner) 
                                            "Device Owner Active" 
                                        else "Device Owner Not Set",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (permissionStatus.isDeviceOwner) 
                                        "✅ Full security features enabled including kiosk mode and advanced restrictions."
                                    else "⚠️ Basic device admin only. For full security features, set up device owner using ADB commands (see setup guide).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        permissionStatus = checkPermissions().copy(
                            notificationSkipped = notificationSkipped,
                            overlaySkipped = overlaySkipped
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh")
                }
                
                Button(
                    onClick = onContinue,
                    enabled = permissionStatus.hasMinimumPermissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (permissionStatus.allGranted) "Continue" else "Continue (Basic)")
                }
            }
            

            
            if (!permissionStatus.hasMinimumPermissions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "⚠️ Minimum permissions (Basic, Notification/Skip, Service, Device Admin) must be granted or skipped before registration",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (!permissionStatus.allGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            permissionStatus.notificationSkipped && permissionStatus.overlaySkipped -> 
                                "ℹ️ Both notification and System Alert Window permissions were skipped. Some features may be limited."
                            permissionStatus.notificationSkipped -> 
                                "ℹ️ Notification permission was skipped. You may miss important alerts."
                            permissionStatus.overlaySkipped -> 
                                "ℹ️ System Alert Window permission was skipped. Some UI features may be limited."
                            else -> 
                                "ℹ️ Some permissions are still pending. You can grant them later in settings."
                        },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    isDeviceOwner: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (!isGranted) {
                TextButton(
                    onClick = onClick
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun PermissionItemWithSkip(
    title: String,
    description: String,
    isGranted: Boolean,
    isSkipped: Boolean,
    onClick: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isGranted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isSkipped -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    isGranted -> Icons.Default.Check
                    isSkipped -> Icons.Default.Warning
                    else -> Icons.Default.Close
                },
                contentDescription = null,
                tint = when {
                    isGranted -> Color(0xFF4CAF50)
                    isSkipped -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isSkipped) "$description (Skipped)" else description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (!isGranted && !isSkipped) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip
                    ) {
                        Text("Skip")
                    }
                    Button(
                        onClick = onClick
                    ) {
                        Text("Grant")
                    }
                }
            }
        }
    }
}