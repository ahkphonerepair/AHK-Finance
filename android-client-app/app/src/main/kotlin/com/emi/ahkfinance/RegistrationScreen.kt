package com.emi.ahkfinance

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Call
import android.app.DatePickerDialog

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import android.telephony.TelephonyManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.*
import android.content.Intent
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import java.security.MessageDigest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    val deviceId = getDeviceId(context)
    
    // Function to request device admin
    fun requestDeviceAdmin() {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, com.emi.ahkfinance.DeviceAdminReceiver::class.java)
            
            android.util.Log.d("DeviceAdmin", "Checking device admin status...")
            
            // Check device owner status
            val isDeviceOwner = dpm.isDeviceOwnerApp(context.packageName)
            android.util.Log.d("DeviceAdmin", "Is Device Owner: $isDeviceOwner")
            
            if (!dpm.isAdminActive(adminComponent)) {
                android.util.Log.d("DeviceAdmin", "Device admin not active, requesting permission...")
                
                if (isDeviceOwner) {
                    android.widget.Toast.makeText(context, "ডিভাইস মালিক হিসেবে অ্যাডমিন অনুমতির জন্য অনুরোধ করা হচ্ছে...", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "ডিভাইস অ্যাডমিন অনুমতির জন্য অনুরোধ করা হচ্ছে...\n\nসম্পূর্ণ কার্যকারিতার জন্য ডিভাইস মালিক সেটআপ প্রয়োজন।", android.widget.Toast.LENGTH_LONG).show()
                }
                
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "ডিভাইস লক ও সুরক্ষার জন্য অ্যাডমিন অনুমতি প্রয়োজন। সম্পূর্ণ নিরাপত্তার জন্য ডিভাইস মালিক সেটআপ করুন।")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                android.util.Log.d("DeviceAdmin", "Device admin request sent")
            } else {
                android.util.Log.d("DeviceAdmin", "Device admin already active")
                if (isDeviceOwner) {
                    android.widget.Toast.makeText(context, "ডিভাইস মালিক ও অ্যাডমিন সক্রিয় - সম্পূর্ণ নিরাপত্তা সক্রিয়", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "ডিভাইস অ্যাডমিন সক্রিয় - উন্নত নিরাপত্তার জন্য ডিভাইস মালিক সেটআপ করুন", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceAdmin", "Failed to request device admin: ${e.message}")
            android.widget.Toast.makeText(context, "ডিভাইস অ্যাডমিন অনুরোধে ত্রুটি: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    // Auto-populate device model
    LaunchedEffect(Unit) {
        deviceModel = getDeviceModelWithBrand()
    }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showDueDetailsDialog by remember { mutableStateOf(false) }
    var dueDetails by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "নিবন্ধন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "ডিভাইস নিবন্ধন করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "গ্রাহকের তথ্য",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                

                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("৪-সংখ্যার আনলক পিন") },
                    leadingIcon = {
                        Text(
                            text = "🔒",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                

                
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it },
                    label = { Text("Device Model") },
                    leadingIcon = {
                        Text(
                            text = "📲",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = {},
                    label = { Text("ডিভাইস আইডি (স্বয়ংক্রিয়)") },
                    leadingIcon = {
                        Text(
                            text = "🆔",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = false
                )
            }
        }
        

        
        // Action Button Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        // Validation
                        when {
                            pin.length != 4 -> {
                                showError = true
                                errorMessage = "৪-সংখ্যার পিন লিখুন"
                            }
                            deviceModel.isBlank() -> {
                                showError = true
                                errorMessage = "ডিভাইস মডেল লিখুন"
                            }
                            else -> {
                                showError = false
                                android.util.Log.d("Registration", "Starting registration process...")
                                
                                val hashedPin = hashPin(pin)
                                saveRegistrationData(context, hashedPin, deviceId, deviceModel)
                                
                                android.util.Log.d("Registration", "Data saved, initializing Firebase...")
                                FirebaseModule.init(context)
                                FirebaseModule.updateDeviceStatus(deviceId, mapOf(
                                    "deviceId" to deviceId,
                                    "deviceModel" to deviceModel,
                                    "pinHash" to hashedPin,
                                    "locked" to false,
                                    "offlineUnlockCount" to 0
                                ))
                                
                                android.util.Log.d("Registration", "Starting LockService...")
                                val serviceIntent = Intent(context, LockService::class.java)
                                ContextCompat.startForegroundService(context, serviceIntent)
                                
                                // Request Device Admin activation
                                android.util.Log.d("Registration", "Requesting device admin permission...")
                                requestDeviceAdmin()
                                
                                // Navigate to status screen after successful registration
                                android.util.Log.d("Registration", "Navigating to status screen...")
                                navController.navigate("status")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "নিবন্ধন করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (showError) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    

}

@Suppress("HardwareIds")
fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

@Suppress("MissingPermission", "HardwareIds")
fun getDeviceIMEI(context: Context): String {
    return try {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.w("IMEI", "READ_PHONE_STATE permission not granted")
            return ""
        }
        
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Try to get IMEI from slot 0, then slot 1 if needed
            val imei0 = telephonyManager.getImei(0)
            if (!imei0.isNullOrEmpty()) {
                android.util.Log.d("IMEI", "Got IMEI from slot 0: ${imei0.take(4)}...")
                return imei0
            }
            
            // Try slot 1 if slot 0 is empty
            val imei1 = telephonyManager.getImei(1)
            if (!imei1.isNullOrEmpty()) {
                android.util.Log.d("IMEI", "Got IMEI from slot 1: ${imei1.take(4)}...")
                return imei1
            }
            
            android.util.Log.w("IMEI", "No IMEI found in either slot")
            return ""
        } else {
            @Suppress("DEPRECATION")
            val deviceId = telephonyManager.deviceId
            if (!deviceId.isNullOrEmpty()) {
                android.util.Log.d("IMEI", "Got device ID: ${deviceId.take(4)}...")
                return deviceId
            }
            
            android.util.Log.w("IMEI", "No device ID found")
            return ""
        }
    } catch (e: SecurityException) {
        android.util.Log.e("IMEI", "Security exception getting IMEI: ${e.message}")
        return ""
    } catch (e: Exception) {
        android.util.Log.e("IMEI", "Error getting IMEI: ${e.message}")
        return ""
    }
}

fun getDeviceModelWithBrand(): String {
    return "${Build.BRAND} ${Build.MODEL}"
}

fun saveRegistrationData(context: Context, hashedPin: String, deviceId: String, deviceModel: String) {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "ahk_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    with(sharedPrefs.edit()) {
        putString("pin", hashedPin)
        putString("deviceId", deviceId)
        putString("deviceModel", deviceModel)
        putBoolean("isLocked", false)
        putInt("offlineUnlockCount", 0)
        putString("paymentLink", FirebaseModule.getDefaultPaymentLink())
        putLong("paymentLinkLastUpdated", System.currentTimeMillis())
        apply()
    }
}


