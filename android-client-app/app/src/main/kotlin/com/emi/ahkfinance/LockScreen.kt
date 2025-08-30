package com.emi.ahkfinance

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.view.View
import androidx.activity.compose.BackHandler
import android.view.WindowManager.LayoutParams

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(navController: NavController) {
    val context = LocalContext.current
    var isLocked by remember { mutableStateOf(true) }
    val deviceId = getDeviceId(context)
    val scope = rememberCoroutineScope()
    var showCallDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showPaymentView by remember { mutableStateOf(false) }
    var paymentUrl by remember { mutableStateOf("") }
    var showPaymentInstructionDialog by remember { mutableStateOf(false) }
    
    // KeyguardManager to check native lock screen state
    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    var isDeviceSecure by remember { mutableStateOf(false) }
    var isKeyguardLocked by remember { mutableStateOf(true) }
    
    // Load device information from EncryptedSharedPreferences
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
    
    val dueAmount = prefs.getString("dueAmount", "0.00") ?: "0.00"
    val dueDate = prefs.getString("dueDate", "YYYY-MM-DD") ?: "YYYY-MM-DD"
    val imei = prefs.getString("imei", "") ?: ""
    val customerName = prefs.getString("customerName", "") ?: ""
    val customerPhone = prefs.getString("customerPhone", "") ?: ""
    val deviceModel = prefs.getString("deviceModel", "") ?: ""
    val offlineUnlockCount = prefs.getInt("offlineUnlockCount", 0)
    val dueDetails = prefs.getString("dueDetails", "") ?: ""
    
    // Log keyguard state for debugging but always show lock screen UI
    if (isDeviceSecure && isKeyguardLocked) {
        Log.d("LockScreen", "Native keyguard is locked, but showing app lock screen anyway")
    }
    
    // Get screen dimensions for responsive design
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenHeight < 600.dp || screenWidth < 360.dp
    val isLargeScreen = screenHeight > 800.dp && screenWidth > 400.dp
    
    // Calculate responsive sizes - optimized for compact layout
    val headerTextSize = when {
        isSmallScreen -> MaterialTheme.typography.headlineSmall
        isLargeScreen -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.headlineSmall
    }
    
    val cardPadding = when {
        isSmallScreen -> 8.dp
        isLargeScreen -> 10.dp
        else -> 8.dp
    }
    
    val keyboardButtonSize = when {
        isSmallScreen -> 40.dp
        isLargeScreen -> 45.dp
        else -> 42.dp
    }
    
    val spacingUnit = when {
        isSmallScreen -> 4.dp
        isLargeScreen -> 6.dp
        else -> 5.dp
    }

    // Check native authentication state on activity resume
    LaunchedEffect(Unit) {
        isDeviceSecure = keyguardManager.isDeviceSecure
        isKeyguardLocked = keyguardManager.isKeyguardLocked
        
        // If device has security and keyguard is still locked, don't show app lock screen
        if (isDeviceSecure && isKeyguardLocked) {
            Log.d("LockScreen", "Native keyguard is still locked, waiting for authentication")
            return@LaunchedEffect
        }
        
        Log.d("LockScreen", "Native authentication verified, proceeding with app lock screen")
    }
    
    // Handle activity lifecycle changes
    DisposableEffect(Unit) {
        val activity = context as? Activity
        
        onDispose {
            // Clean up any resources when the composable is disposed
            Log.d("LockScreen", "LockScreen composable disposed")
        }
    }
    
    // Monitor keyguard state changes and handle edge cases
    LaunchedEffect(Unit) {
        while (true) {
            val wasKeyguardLocked = isKeyguardLocked
            val wasDeviceSecure = isDeviceSecure
            
            isKeyguardLocked = keyguardManager.isKeyguardLocked
            isDeviceSecure = keyguardManager.isDeviceSecure
            
            // Handle edge case: device security settings changed
            if (wasDeviceSecure != isDeviceSecure) {
                Log.d("LockScreen", "Device security settings changed: $isDeviceSecure")
            }
            
            // Handle normal unlock case
            if (wasKeyguardLocked && !isKeyguardLocked) {
                Log.d("LockScreen", "Native keyguard unlocked, app lock screen can now be shown")
            }
            
            // Handle edge case: keyguard was bypassed or disabled
            if (!isDeviceSecure && !isKeyguardLocked) {
                Log.d("LockScreen", "Device has no security or keyguard disabled, allowing app lock screen")
            }
            
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }

    // Listen for remote lock/unlock
    LaunchedEffect(deviceId) {
        FirebaseModule.listenForLockUnlock(deviceId,
            onLock = { isLocked = true },
            onUnlock = { isLocked = false }
        )
    }

    // Check local lock state using EncryptedSharedPreferences and monitor for changes
    LaunchedEffect(Unit) {
        while (true) {
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
                val currentLockState = prefs.getBoolean("isLocked", true)
                if (currentLockState != isLocked) {
                    Log.d("LockScreen", "Lock state changed: $isLocked -> $currentLockState")
                    isLocked = currentLockState
                }
            } catch (e: Exception) {
                Log.e("LockScreen", "Error reading lock state from EncryptedSharedPreferences: ${e.message}")
                // Fallback to regular SharedPreferences
                val prefs = context.getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
                val currentLockState = prefs.getBoolean("isLocked", true)
                if (currentLockState != isLocked) {
                    Log.d("LockScreen", "Fallback lock state changed: $isLocked -> $currentLockState")
                    isLocked = currentLockState
                }
            }
            
            // Check every 500ms for lock state changes
            kotlinx.coroutines.delay(500)
        }
    }

    if (!isLocked) {
        // Clean up window flags before navigation
        LaunchedEffect(Unit) {
            (context as? Activity)?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                
                // Restore normal system UI
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            
            // Check if device is registered to determine navigation
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
            val isRegistered = prefs.getString("deviceId", null) != null
            
            Log.d("LockScreen", "Device unlocked, navigating away from lock screen")
            
            if (isRegistered) {
                navController.navigate("status") {
                    popUpTo("lock") { inclusive = true }
                }
            } else {
                navController.navigate("registration") {
                    popUpTo("lock") { inclusive = true }
                }
            }
            
            // Finish the activity to ensure it's completely closed
            (context as? Activity)?.finish()
        }
        return
    }

    // Set essential window flags for lock screen
    val activity = context as? Activity
    LaunchedEffect(Unit) {
        activity?.window?.let { window ->
            // Essential flags for lock screen functionality
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            
            // Set secure flag to prevent screenshots
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            
            // Set immersive full-screen mode to hide status bar and navigation bar
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }
    
    // Clean up window flags when leaving lock screen
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                
                // Restore normal system UI visibility
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
    // Block back button
    BackHandler(true) { /* Do nothing */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Deep blue
                        Color(0xFF3B82F6), // Samsung blue
                        Color(0xFF60A5FA)  // Light blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(cardPadding)
                .padding(bottom = if (isSmallScreen) 12.dp else if (isLargeScreen) 16.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacingUnit)
        ) {
             // Payment Status Card
             Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Brand Header
                    Text(
                        "AHK Finance",
                        style = headerTextSize.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1E3A8A)
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        "মোবাইল সেবা নিন সহজ কিস্তিতে",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF64748B)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Payment Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "⚠️ পেমেন্ট বকেয়া",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFDC2626)
                            )
                            
                            Spacer(Modifier.height(6.dp))
                            
                            Text(
                                "আপনার মোবাইলের বাকি টাকা পরিশোধের সময়সীমা অতিবাহিত হয়ে গেছে। এই কারণে আপনার ফোনটি লক করে দেয়া হয়েছে। দয়া করে বাকি টাকা পরিশোধ করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF374151),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            Text(
                                "পেমেন্ট সম্পন্ন হওয়ার পর স্ক্রিন লক সরিয়ে ফেলা হবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Legal Warning Section
            Card(
                onClick = {
                    showDetailsDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF2F2)
                ),
                border = BorderStroke(2.dp, Color(0xFFDC2626)),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp
                )
            ) {
                // Main warning text
                Text(
                    "এই ডিভাইসটি AHK Finance এর আর্থিক চুক্তির অধীনে রয়েছে। চুক্তি অনুযায়ী বকেয়া কিস্তি পরিশোধ না করে ডিভাইসের লক খোলার চেষ্টা করা বাংলাদেশ সাইবার নিরাপত্তা আইন ২০২৩ অনুযায়ী একটি দণ্ডনীয় অপরাধ।\n\n👉 এ ধরনের অবৈধ কার্যক্রমে জড়িত হলে ব্যবহারকারী ও সংশ্লিষ্ট সহযোগীর বিরুদ্ধে অবিলম্বে আইনগত ব্যবস্থা গ্রহণ করা হবে। \n\n📌 বিস্তারিত জানতে এখানে ক্লিক করুন।",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    ),
                    color = Color(0xFF374151),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
            
            // Device Information Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Device Status
                    InfoRow("বকেয়া টাকার পরিমাণ", "৳ $dueAmount")
                    InfoRow("পরবর্তী বিলিং তারিখ", dueDate)
                    InfoRow("IMEI নম্বর", imei)
                    InfoRow("গ্রাহকের নাম", customerName)
                    InfoRow("ফোন নম্বর", customerPhone)
                    InfoRow("ডিভাইস মডেল", deviceModel)
                    
                    if (dueDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "বাকির কারন:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF1E3A8A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dueDetails,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF374151)
                        )
                    }
 
                 }
            }
            
            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else if (isLargeScreen) 6.dp else 5.dp)
            ) {
                // Unlock Button
                Card(
                    onClick = {
                        showUnlockDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(if (isSmallScreen) 35.dp else if (isLargeScreen) 40.dp else 38.dp),
                    shape = RoundedCornerShape(if (isSmallScreen) 25.dp else if (isLargeScreen) 35.dp else 30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSmallScreen) 4.dp else if (isLargeScreen) 8.dp else 6.dp,
                        pressedElevation = if (isSmallScreen) 8.dp else if (isLargeScreen) 16.dp else 12.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "আনলক করুন",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
                
                // Call Support Button
                Card(
                    onClick = {
                        showCallDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(if (isSmallScreen) 35.dp else if (isLargeScreen) 40.dp else 38.dp),
                    shape = RoundedCornerShape(if (isSmallScreen) 25.dp else if (isLargeScreen) 35.dp else 30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    ),
                    border = BorderStroke(if (isSmallScreen) 1.5.dp else 2.dp, Color(0xFF3B82F6)),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSmallScreen) 3.dp else if (isLargeScreen) 6.dp else 4.dp,
                        pressedElevation = if (isSmallScreen) 6.dp else if (isLargeScreen) 12.dp else 8.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Call Support",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(if (isSmallScreen) 20.dp else if (isLargeScreen) 28.dp else 24.dp)
                            )
                            Spacer(Modifier.width(if (isSmallScreen) 8.dp else if (isLargeScreen) 16.dp else 12.dp))
                            Text(
                                "সাপোর্ট কল করুন",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }
                
                // Payment Button - Card with Bengali text
                Card(
                    onClick = {
                        showPaymentInstructionDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(if (isSmallScreen) 35.dp else if (isLargeScreen) 40.dp else 38.dp),
                    shape = RoundedCornerShape(if (isSmallScreen) 25.dp else if (isLargeScreen) 35.dp else 30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF22C55E) // Green background
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSmallScreen) 4.dp else if (isLargeScreen) 8.dp else 6.dp,
                        pressedElevation = if (isSmallScreen) 8.dp else if (isLargeScreen) 16.dp else 12.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💳 পেমেন্ট করুন",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            


            
            // Persistent watermark at bottom of scrollable content
            Spacer(Modifier.height(if (isSmallScreen) 4.dp else 6.dp))
            Text(
                "AHK Finance | Development by AHK Phone Repair",
                color = Color.LightGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isSmallScreen) 8.dp else if (isLargeScreen) 12.dp else 10.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    
    // Call Support Dialog
    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            title = {
                Text(
                    text = "সাপোর্ট নম্বর",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF3B82F6)
                )
            },
            text = {
                Text(
                    text = "call 01630138471",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showCallDialog = false }
                ) {
                    Text(
                        "ঠিক আছে",
                        color = Color(0xFF3B82F6),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    // Details Dialog
    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Text(
                    text = "আইনগত সতর্কতা",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "বাংলাদেশের সাইবার নিরাপত্তা আইন, ২০২৩-এর ৩১ ও ৩২ ধারার আওতায়, কোনো ব্যক্তি বকেয়া কিস্তি পরিশোধ না করেই ডিভাইসের লক অবৈধভাবে খোলার চেষ্টা করলে এটি একটি হ্যাকিং সংক্রান্ত দণ্ডনীয় অপরাধ। এর জন্য সর্বোচ্চ ১৪ বছর কারাদণ্ড অথবা এক কোটি টাকা জরিমানা, বা উভয় শাস্তি যেতে পারে। একইভাবে, অপরাধে সহায়তা করলেও একই ধরনের আইনগত ব্যবস্থা গ্রহণ করা হবে ।\n\n📌 আপনার সেবা চালু রাখতে দ্রুত বকেয়া টাকা পরিশোধ করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false }
                ) {
                    Text(
                        "বুঝেছি",
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    // Unlock Dialog
    if (showUnlockDialog) {
        UnlockDialog(
            onDismiss = { showUnlockDialog = false },
            onUnlockSuccess = {
                showUnlockDialog = false
                scope.launch {
                    try {
                        // Update local preferences
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
                        
                        // Increment offline unlock count
                        val currentCount = prefs.getInt("offlineUnlockCount", 0)
                        val newCount = currentCount + 1
                        
                        prefs.edit()
                            .putBoolean("isLocked", false)
                            .putInt("offlineUnlockCount", newCount)
                            .apply()
                        
                        // Update Firebase with unlock status and offline unlock count
                        FirebaseModule.updateDeviceStatus(deviceId, mapOf(
                            "locked" to false,
                            "offlineUnlockCount" to newCount,
                            "lastOfflineUnlock" to System.currentTimeMillis()
                        ))
                        
                        // Add a small delay to ensure preferences are fully written
                        kotlinx.coroutines.delay(100)
                         
                         // Navigate to status screen
                         navController.navigate("status")
                    } catch (e: Exception) {
                        android.util.Log.e("LockScreen", "Error during unlock: ${e.message}")
                    }
                }
            }
        )
    }
    
    // Payment Instruction Dialog
    if (showPaymentInstructionDialog) {
        PaymentInstructionDialog(
            deviceId = deviceId,
            onDismiss = { showPaymentInstructionDialog = false },
            onPaymentClick = {
                showPaymentInstructionDialog = false
                scope.launch {
                    try {
                        val url = PaymentManager.getPaymentLink(context)
                        if (!url.isNullOrEmpty()) {
                            paymentUrl = url
                            showPaymentView = true
                        } else {
                            Log.e("LockScreen", "Failed to get payment URL")
                        }
                    } catch (e: Exception) {
                        Log.e("LockScreen", "Error getting payment URL: ${e.message}")
                    }
                }
            }
        )
    }
    
    // Integrated Payment View
    if (showPaymentView && paymentUrl.isNotEmpty()) {
        PaymentWebView(
            paymentUrl = paymentUrl,
            onClose = {
                showPaymentView = false
                paymentUrl = ""
            },
            onPaymentComplete = {
                showPaymentView = false
                paymentUrl = ""
                // Optionally refresh device status or navigate
            },
            onPaymentFailed = { error ->
                showPaymentView = false
                paymentUrl = ""
                Log.e("LockScreen", "Payment failed: $error")
            }
        )
    }
}

@Composable
fun CustomNumericKeyboard(
    buttonSize: Dp = 70.dp,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy((buttonSize * 0.15f))
    ) {
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { number ->
                    if (number.isNotEmpty()) {
                        KeyboardButton(
                            text = number,
                            size = buttonSize,
                            onClick = {
                                if (number == "⌫") {
                                    onDeleteClick()
                                } else {
                                    onNumberClick(number)
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(buttonSize))
                    }
                }
            }
        }
    }
}

@Composable
fun UnlockDialog(
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val correctPin = loadPin(context)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Offline Unlock",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter PIN to unlock device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // PIN Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (index < pinInput.length) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < pinInput.length) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onPrimary,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
                
                if (error) {
                    Text(
                        text = "Incorrect PIN. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Custom Numeric Keyboard
                CustomNumericKeyboard(
                    onNumberClick = { number ->
                        if (pinInput.length < 4) {
                            pinInput += number
                            error = false
                        }
                    },
                    onDeleteClick = {
                        if (pinInput.isNotEmpty()) {
                            pinInput = pinInput.dropLast(1)
                            error = false
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pinInput.length == 4) {
                        if (hashPin(pinInput) == correctPin) {
                            onUnlockSuccess()
                        } else {
                            error = true
                            pinInput = ""
                        }
                    }
                },
                enabled = pinInput.length == 4
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun KeyboardButton(
    text: String,
    size: Dp = 70.dp,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = RoundedCornerShape((size * 0.23f)),
        colors = CardDefaults.cardColors(
            containerColor = if (text == "⌫") Color(0xFFEF4444) else Color(0xFF3B82F6)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = (size * 0.06f),
            pressedElevation = (size * 0.11f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
fun PaymentInstructionDialog(
    deviceId: String,
    onDismiss: () -> Unit,
    onPaymentClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "পেমেন্ট নির্দেশনা",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF3B82F6)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bengali instruction message
                Text(
                    text = "প্রথমে আপনার বাকি পরিমাণ কতো সেটা দেখে নিন। এরপর আপনার Device ID কপি করে নিন, তারপর Bkash Payment বাটনে ক্লিক করুন। Enter Amount অপশনে আপনার বাকির পরিমাণ লিখুন, Payment Reference অপশনে আপনার পূর্বের কপি করা Device ID পেস্ট করুন, আপনার বিকাশ নম্বর এবং কন্টাক্ট নম্বর যদি এক না হয় তবে ( [✓]Use bKash wallet number as your contact number) থেকে টিক চিহ্ন তুলে দিন এবং আপনার কন্টাক্ট নম্বর লিখুন। তারপর আপনার Bkash নম্বর, ওটিপি, পিন দিয়ে পেমেন্ট সম্পন্ন করুন। সাহায্য অথবা বিস্তারিত জানতে 01630138471 নম্বরে Whatsapp ম্যাসেজ করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                
                // Device ID with copy feature
                Card(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(deviceId))
                        Toast.makeText(context, "Device ID কপি হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3F4F6)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Device ID (ক্লিক করে কপি করুন):",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = deviceId,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1F2937)
                        )
                    }
                }
                
                // Image button for payment
                Image(
                    painter = painterResource(id = R.drawable.b),
                    contentDescription = "Bkash Payment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable { onPaymentClick() },
                    contentScale = ContentScale.Fit
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "বন্ধ করুন",
                    color = Color(0xFF3B82F6),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

fun loadPin(context: Context): String? {
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
    return sharedPrefs.getString("pin", null)
}

fun hashPin(pin: String): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
