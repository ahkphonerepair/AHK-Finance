# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore - critical for anti-uninstall database sync
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.** { *; }
-keep class com.google.firebase.firestore.ListenerRegistration { *; }
-keep class com.google.firebase.firestore.DocumentSnapshot { *; }
-keep class com.google.firebase.firestore.QuerySnapshot { *; }
# Keep Firestore callback interfaces
-keep interface com.google.firebase.firestore.EventListener { *; }
-keep interface com.google.android.gms.tasks.OnSuccessListener { *; }
-keep interface com.google.android.gms.tasks.OnFailureListener { *; }
-keep interface com.google.android.gms.tasks.OnCompleteListener { *; }

# FCM
-keep class com.google.firebase.messaging.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Security Crypto - critical for encrypted preferences used by anti-uninstall
-keep class androidx.security.crypto.** { *; }
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }
-keep class androidx.security.crypto.MasterKey { *; }
-keep class androidx.security.crypto.MasterKey$* { *; }
-dontwarn androidx.security.crypto.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep all model classes
-keep class com.emi.ahkfinance.** { *; }

# Device Admin Receiver - must be kept for Android system to find it
-keep class com.emi.ahkfinance.DeviceAdminReceiver { *; }
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# Anti-Uninstall Manager - critical for anti-uninstall functionality
-keep class com.emi.ahkfinance.AntiUninstallManager { *; }
-keep class com.emi.ahkfinance.AntiUninstallManager$* { *; }

# Accessibility Control Service - critical for anti-uninstall functionality
-keep class com.emi.ahkfinance.AccessibilityControlService { *; }
-keep class com.emi.ahkfinance.AccessibilityControlService$* { *; }

# Accessibility Service - must be kept for Android system
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Device Policy Manager related classes
-keep class android.app.admin.** { *; }
-dontwarn android.app.admin.**

# Keep device admin methods and callbacks
-keepclassmembers class * extends android.app.admin.DeviceAdminReceiver {
    public void onEnabled(android.content.Context, android.content.Intent);
    public void onDisabled(android.content.Context, android.content.Intent);
    public void onPasswordChanged(android.content.Context, android.content.Intent);
    public void onPasswordFailed(android.content.Context, android.content.Intent);
    public void onPasswordSucceeded(android.content.Context, android.content.Intent);
}

# Keep all classes with @Keep annotation
-keep @androidx.annotation.Keep class * { *; }

# Keep all enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}