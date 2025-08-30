# AHK Finance Project Configuration Guide

This comprehensive guide explains how to modify key parameters in the AHK Finance project for customization. The project consists of an Android client app and an admin app for managing EMI (Equated Monthly Installment) devices.

## Table of Contents

1. [Project Structure](#project-structure)
2. [Support Phone Number Configuration](#support-phone-number-configuration)
3. [Payment Link Configuration](#payment-link-configuration)
4. [Warning Message Content](#warning-message-content)
5. [Lockscreen Message Content](#lockscreen-message-content)
6. [Payment Dialog Message Configuration](#payment-dialog-message-configuration)
7. [App Branding Configuration](#app-branding-configuration)
8. [APK Signature Configuration](#apk-signature-configuration)
9. [Testing Procedures](#testing-procedures)
10. [Impact Analysis](#impact-analysis)

## Project Structure

The project contains two main applications:

```
ahkemi/
├── android-client-app/          # Main EMI client application
│   └── app/src/main/
│       ├── kotlin/com/emi/ahkfinance/
│       └── res/values/
└── admin-app/                   # Admin management application
    └── app/src/main/
        ├── kotlin/com/emi/ahkadmin/
        └── res/values/
```

## Support Phone Number Configuration

### 1. Lockscreen Activity Phone Number

**Location:** `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/LockScreen.kt`

**Current Configuration:**
```kotlin
// Line ~388: Hardcoded phone number in lockscreen
val phoneNumber = "01XXXXXXXXX"
```

**Parameter Details:**
- **Variable Name:** `phoneNumber` (hardcoded)
- **Format:** Bangladeshi mobile number format (11 digits starting with 01)
- **Current Value:** `01XXXXXXXXX`
- **Location in Code:** Line 388 in `LockScreen.kt`

**How to Modify:**
1. Open `LockScreen.kt`
2. Locate the hardcoded phone number `01XXXXXXXXX`
3. Replace with your desired support number
4. Ensure the format follows Bangladeshi mobile standards

**Example:**
```kotlin
// Before
val phoneNumber = "01XXXXXXXXX"

// After
val phoneNumber = "01712345678"
```

### 2. Payment Dialog Phone Numbers

**Location:** `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/FirebaseModule.kt`

**Current Configuration:**
```kotlin
// Line 12: Default payment link contains phone number
private const val DEFAULT_PAYMENT_LINK = "https://shop.bkash.com/ahk-phone-repair01XXXXXXXXX/paymentlink"
```

**Parameter Details:**
- **Constant Name:** `DEFAULT_PAYMENT_LINK`
- **Format:** bKash payment URL with embedded phone number
- **Current Value:** Contains `01XXXXXXXXX`
- **Location:** Line 12 in `FirebaseModule.kt`

## Payment Link Configuration

### Default Payment Link Setup

**Location:** `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/FirebaseModule.kt`

**Configuration Details:**
```kotlin
private const val DEFAULT_PAYMENT_LINK = "https://shop.bkash.com/ahk-phone-repair01XXXXXXXXX/paymentlink"
```

**Parameter Specifications:**
- **Variable:** `DEFAULT_PAYMENT_LINK`
- **Type:** Private constant string
- **Format Requirements:**
  - Must be HTTPS URL
  - Supported domains: `bkash.com`, `nagad.com.bd`
  - URL validation is performed in `PaymentManager.kt`

**Validation Logic:**
```kotlin
// PaymentManager.kt validates payment links
fun isValidPaymentLink(link: String): Boolean {
    return link.startsWith("https://") && 
           (link.contains("bkash.com") || link.contains("nagad.com.bd"))
}
```

**How to Modify:**
1. Open `FirebaseModule.kt`
2. Locate `DEFAULT_PAYMENT_LINK` constant
3. Replace with your payment URL
4. Ensure URL meets validation requirements

**Valid Examples:**
```kotlin
// bKash payment link
private const val DEFAULT_PAYMENT_LINK = "https://shop.bkash.com/your-merchant-id/paymentlink"

// Nagad payment link
private const val DEFAULT_PAYMENT_LINK = "https://nagad.com.bd/your-merchant/payment"
```

### Dynamic Payment Link Updates

The system supports runtime payment link updates through Firebase:

**Functions Available:**
- `updatePaymentLink(deviceId, paymentLink, onSuccess, onFailure)`
- `getPaymentLink(deviceId, onSuccess, onFailure)`
- `listenForPaymentLinkChanges(deviceId, onPaymentLinkChanged)`

## Warning Message Content

### 1. Permission Warning Messages

**Location:** `android-client-app/app/src/main/res/values/strings.xml`

**Current Configuration:**
```xml
<string name="all_permissions_required">⚠️ All permissions must be granted before registration</string>
```

**Parameter Details:**
- **String Name:** `all_permissions_required`
- **Format:** Unicode warning emoji + English text
- **Usage:** Permission validation screens

### 2. Device Admin Warning

**Location:** `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/PermissionActivity.kt`

**Current Configuration:**
```kotlin
// Line 415: Device admin warning
"⚠️ Basic device admin only. For full security features, set up device owner using ADB commands (see setup guide)."

// Line 464: Minimum permissions warning
"⚠️ Minimum permissions (Basic, Notification/Skip, Service, Device Admin) must be granted or skipped before registration"
```

### 3. Anti-Uninstall Warnings

**Location:** `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/AccessibilityControlService.kt`

**Current Configuration:**
```kotlin
// Line 305: Uninstall attempt detection
Log.w(TAG, "[ANTI-UNINSTALL] ⚠️ UNINSTALL ATTEMPT DETECTED! Blocking action...")

// Line 317: Uninstall keywords detection
Log.w(TAG, "[ANTI-UNINSTALL] ⚠️ UNINSTALL KEYWORDS DETECTED: $eventText")
```

## Lockscreen Message Content

### 1. Main Lock Message

**Location:** `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/LockScreen.kt`

**Current Configuration:**
```kotlin
// Line 388: Payment due warning
"⚠️ পেমেন্ট বকেয়া"

// Line 437: Legal warning text (Bengali)
"এই ডিভাইসটি AHK Finance এর আর্থিক চুক্তির অধীনে রয়েছে। চুক্তি অনুযায়ী বকেয়া কিস্তি পরিশোধ না করে ডিভাইসের লক খোলার চেষ্টা করা বাংলাদেশ সাইবার নিরাপত্তা আইন ২০২৩ অনুযায়ী একটি দণ্ডনীয় অপরাধ।\n\n👉 এ ধরনের অবৈধ কার্যক্রমে জড়িত হলে ব্যবহারকারী ও সংশ্লিষ্ট সহযোগীর বিরুদ্ধে অবিলম্বে আইনগত ব্যবস্থা গ্রহণ করা হবে। \n\n📌 বিস্তারিত জানতে এখানে ক্লিক করুন।"
```

**Parameter Details:**
- **Warning Title:** `"⚠️ পেমেন্ট বকেয়া"` (Payment Due)
- **Legal Text:** Multi-line Bengali legal warning
- **Format:** Unicode emojis + Bengali text with line breaks
- **Location:** Hardcoded in `LockScreen.kt`

### 2. Lock Message from Strings

**Location:** `android-client-app/app/src/main/res/values/strings.xml`

**Current Configuration:**
```xml
<string name="lock_message">Your device is locked due to payment issues. Please contact support.</string>
```

**Parameter Details:**
- **String Name:** `lock_message`
- **Format:** Plain English text
- **Usage:** Alternative lock message

### 3. Company Branding in Lock Screen

**Location:** `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/LockScreen.kt`

**Current Configuration:**
```kotlin
// Line 358: Company name
"AHK Finance"

// Line 611: Footer branding
"AHK Finance | Development by AHK Phone Repair"
```

## Payment Dialog Message Configuration

### Overview
The payment dialog messages are displayed when users interact with payment-related features on the lock screen. These messages provide instructions and guidance for completing payments.

### File Location
- **File**: `android-client-app/app/src/main/kotlin/com/emi/ahkfinance/LockScreen.kt`
- **Component**: `PaymentInstructionDialog` composable function

### Configurable Messages

#### 1. Payment Dialog Title
```kotlin
Text(
    text = "পেমেন্ট নির্দেশনা",  // Payment Instructions
    style = MaterialTheme.typography.headlineSmall,
    color = Color(0xFF3B82F6)
)
```

#### 2. Payment Instruction Text
```kotlin
Text(
    text = "প্রথমে আপনার বাকি পরিমাণ কতো সেটা দেখে নিন। এরপর আপনার Device ID কপি করে নিন, তারপর Bkash Payment বাটনে ক্লিক করুন। Enter Amount অপশনে আপনার বাকির পরিমাণ লিখুন, Payment Reference অপশনে আপনার পূর্বের কপি করা Device ID পেস্ট করুন, আপনার বিকাশ নম্বর এবং কন্টাক্ট নম্বর যদি এক না হয় তবে ( [✓]Use bKash wallet number as your contact number) থেকে টিক চিহ্ন তুলে দিন এবং আপনার কন্টাক্ট নম্বর লিখুন। তারপর আপনার Bkash নম্বর, ওটিপি, পিন দিয়ে পেমেন্ট সম্পন্ন করুন। সাহায্য অথবা বিস্তারিত জানতে 01XXXXXXXXX নম্বরে Whatsapp ম্যাসেজ করুন।",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurface,
    lineHeight = 20.sp
)
```

**English Translation**: "First, check how much you owe. Then copy your Device ID, then click on Bkash Payment button. Enter your due amount in Enter Amount option, paste your previously copied Device ID in Payment Reference option, if your bKash number and contact number are not the same then remove the tick mark from ([✓]Use bKash wallet number as your contact number) and enter your contact number. Then complete the payment with your Bkash number, OTP, PIN. For help or details, WhatsApp message to 01630138471."

#### 3. Device ID Copy Instructions
```kotlin
Text(
    text = "Device ID (ক্লিক করে কপি করুন):",  // Device ID (Click to copy)
    style = MaterialTheme.typography.bodySmall,
    color = Color(0xFF6B7280)
)
```

#### 4. Copy Success Message
```kotlin
Toast.makeText(context, "Device ID কপি হয়েছে", Toast.LENGTH_SHORT).show()  // Device ID copied
```

#### 5. Dialog Close Button
```kotlin
Text(
    "বন্ধ করুন",  // Close
    color = Color(0xFF3B82F6),
    style = MaterialTheme.typography.labelLarge
)
```

#### 6. Lock Screen Payment Status Messages
```kotlin
// Payment overdue warning
Text(
    text = "⚠️ পেমেন্ট বকেয়া",  // Payment Overdue
    style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold
    ),
    color = Color(0xFFDC2626)
)

// Payment overdue description
Text(
    text = "আপনার মোবাইলের বাকি টাকা পরিশোধের সময়সীমা অতিবাহিত হয়ে গেছে। এই কারণে আপনার ফোনটি লক করে দেয়া হয়েছে। দয়া করে বাকি টাকা পরিশোধ করুন।",
    style = MaterialTheme.typography.bodyMedium,
    color = Color(0xFF374151),
    lineHeight = 20.sp
)
```

**English Translation**: "Your mobile's payment deadline has passed. Your phone has been locked for this reason. Please pay the remaining amount."

#### 7. Action Button Texts
```kotlin
// Payment button
Text(
    text = "💳 পেমেন্ট করুন",  // Make Payment
    style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    color = Color.White
)

// Support call button
Text(
    text = "সাপোর্ট কল করুন",  // Call Support
    style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    color = Color.White
)

// Unlock button
Text(
    text = "আনলক করুন",  // Unlock
    style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    color = Color.White
)
```

#### 8. Unlock Dialog Messages
```kotlin
// Dialog title
Text(
    text = "Offline Unlock",
    style = MaterialTheme.typography.headlineSmall,
    color = MaterialTheme.colorScheme.onSurface
)

// PIN entry instruction
Text(
    text = "Enter PIN to unlock device",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurface
)

// Error message
Text(
    text = "Incorrect PIN. Please try again.",
    color = MaterialTheme.colorScheme.error,
    style = MaterialTheme.typography.bodySmall
)

// Action buttons
Text("Unlock")  // Confirm button
Text("Cancel")  // Dismiss button
```

### Configuration Steps

1. **Locate the LockScreen.kt file**:
   ```
   android-client-app/app/src/main/kotlin/com/emi/ahkfinance/LockScreen.kt
   ```

2. **Find the PaymentInstructionDialog function** (around line 1000)

3. **Modify the Bengali text strings** as needed:
   - Update payment instruction text
   - Change dialog title
   - Modify button labels
   - Update copy instructions

4. **Update support contact number** in the instruction text:
   ```kotlin
   "সাহায্য অথবা বিস্তারিত জানতে 01630138471 নম্বরে Whatsapp ম্যাসেজ করুন।"
   ```

5. **Rebuild the application** after making changes

### Testing Payment Dialog Messages

1. **Trigger Lock Screen**:
   - Use admin app to lock a device
   - Verify lock screen appears with payment status

2. **Test Payment Dialog**:
   - Click "💳 পেমেন্ট করুন" button
   - Verify payment instruction dialog opens
   - Check all Bengali text displays correctly

3. **Test Device ID Copy**:
   - Click on Device ID area
   - Verify toast message appears
   - Check clipboard contains correct Device ID

4. **Test Dialog Actions**:
   - Verify "বন্ধ করুন" button closes dialog
   - Test Bkash payment button functionality

### Impact Analysis

**High Impact Changes**:
- Payment instruction text modifications
- Support contact number updates
- Button label changes

**Medium Impact Changes**:
- Dialog title modifications
- Copy instruction text updates

**Low Impact Changes**:
- Toast message text
- Color scheme adjustments

### Best Practices

1. **Language Consistency**: Maintain consistent Bengali terminology
2. **Character Encoding**: Ensure proper UTF-8 encoding for Bengali text
3. **Text Length**: Keep instruction text concise but comprehensive
4. **Contact Information**: Always verify support contact numbers are current
5. **Testing**: Test on actual devices to ensure Bengali text renders correctly

### Common Issues

1. **Bengali Text Not Displaying**: Check font support and encoding
2. **Text Overflow**: Adjust text size or container dimensions
3. **Copy Function Not Working**: Verify clipboard permissions
4. **Payment Button Not Responding**: Check payment URL configuration

## App Branding Configuration

### 1. Application Name

**Location:** `android-client-app/app/src/main/res/values/strings.xml`

**Current Configuration:**
```xml
<string name="app_name">AHK Finance</string>
```

**Parameter Details:**
- **String Name:** `app_name`
- **Usage:** App launcher, system notifications, title bars
- **Format:** Plain text (no special characters recommended)

### 2. Watermark Text

**Location:** `android-client-app/app/src/main/res/values/strings.xml`

**Current Configuration:**
```xml
<string name="watermark">AHK Finance | মোবাইল সেবা নিন সহজ কিস্তিতে</string>
```

**Parameter Details:**
- **String Name:** `watermark`
- **Format:** English + Bengali mixed text
- **Usage:** Main activity watermark display
- **Translation:** "AHK Finance | Get mobile service in easy installments"

### 3. Notification Titles

**Multiple Locations:**

```kotlin
// LockService.kt - Line 147
.setContentTitle("AHK Finance")

// LocationTrackingService.kt - Line 104
.setContentTitle("AHK Finance - Location Tracking")

// MyFirebaseMessagingService.kt - Line 32
.setContentTitle("AHK Finance")
```

## APK Signature Configuration

### Overview

The AHK Finance project uses digital signatures to ensure APK integrity and authenticity. Two keystore files are available for different signing purposes:

- `ahk-release-key.jks` - Primary release keystore (v1)
- `ahk-release-key-v2.jks` - Updated release keystore (v2)

### Keystore File Locations

**Project Root Directory:**
```
ahkemi/
├── ahk-release-key.jks          # Primary release keystore
├── ahk-release-key-v2.jks       # Updated release keystore
├── admin-app/
└── android-client-app/
```

### Build Configuration

#### Admin App Signing Configuration

**Location:** `admin-app/app/build.gradle`

**Current Configuration:**
```gradle
android {
    signingConfigs {
        release {
            storeFile file('../../ahk-release-key-v2.jks')
            storePassword 'your_store_password'
            keyAlias 'ahk_release_key'
            keyPassword 'your_key_password'
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

#### Client App Signing Configuration

**Location:** `android-client-app/app/build.gradle`

**Current Configuration:**
```gradle
android {
    signingConfigs {
        release {
            storeFile file('../../ahk-release-key-v2.jks')
            storePassword 'your_store_password'
            keyAlias 'ahk_release_key'
            keyPassword 'your_key_password'
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### Keystore Management

#### Creating a New Keystore

**Command:**
```bash
keytool -genkey -v -keystore ahk-release-key-v3.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ahk_release_key
```

**Parameters:**
- **Keystore Name:** `ahk-release-key-v3.jks`
- **Key Algorithm:** RSA
- **Key Size:** 2048 bits
- **Validity:** 10000 days (~27 years)
- **Alias:** `ahk_release_key`

#### Keystore Information

**View Keystore Details:**
```bash
keytool -list -v -keystore ahk-release-key-v2.jks
```

**Verify APK Signature:**
```bash
jarsigner -verify -verbose -certs app-release.apk
```

### Security Best Practices

#### Keystore Protection

1. **Password Security:**
   - Use strong, unique passwords for keystore and key
   - Store passwords securely (not in version control)
   - Consider using environment variables for CI/CD

2. **File Security:**
   - Keep keystore files in secure, backed-up locations
   - Limit access to authorized personnel only
   - Never commit keystore files to public repositories

3. **Backup Strategy:**
   - Maintain multiple secure backups of keystore files
   - Document keystore passwords in secure password manager
   - Test backup restoration procedures regularly

#### Environment Variables Setup

**For CI/CD Integration:**
```gradle
// In build.gradle
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_PATH") ?: "../../ahk-release-key-v2.jks")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS") ?: "ahk_release_key"
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
}
```

**Environment Variables:**
- `KEYSTORE_PATH` - Path to keystore file
- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Key alias name
- `KEY_PASSWORD` - Key password

### Build Commands

#### Manual Release Build

**Admin App:**
```bash
cd admin-app
./gradlew assembleRelease
```

**Client App:**
```bash
cd android-client-app
./gradlew assembleRelease
```

#### Automated Build Script

**Location:** `admin-app/build-release.bat`

```batch
@echo off
echo Building AHK Admin App Release...
call gradlew clean
call gradlew assembleRelease
echo Build completed. Check app/build/outputs/apk/release/
pause
```

### APK Verification

#### Signature Verification

**Check APK Signature:**
```bash
# Using apksigner (Android SDK)
apksigner verify --verbose app-release.apk

# Using jarsigner (Java SDK)
jarsigner -verify -verbose -certs app-release.apk
```

#### APK Analysis

**Get APK Information:**
```bash
# Using aapt (Android Asset Packaging Tool)
aapt dump badging app-release.apk

# Check signing certificate
keytool -printcert -jarfile app-release.apk
```

### Troubleshooting Signing Issues

#### Common Problems

1. **Keystore Not Found:**
   ```
   Error: Keystore file does not exist
   ```
   **Solution:** Verify keystore file path in build.gradle

2. **Invalid Password:**
   ```
   Error: Keystore was tampered with, or password was incorrect
   ```
   **Solution:** Check keystore and key passwords

3. **Key Alias Not Found:**
   ```
   Error: Key alias not found
   ```
   **Solution:** Verify key alias name in configuration

4. **Certificate Expired:**
   ```
   Error: Certificate expired
   ```
   **Solution:** Create new keystore or extend certificate validity

#### Debug Commands

**List Keystore Contents:**
```bash
keytool -list -keystore ahk-release-key-v2.jks
```

**Check Certificate Details:**
```bash
keytool -list -v -keystore ahk-release-key-v2.jks -alias ahk_release_key
```

**Verify Build Configuration:**
```bash
./gradlew signingReport
```

### Migration Between Keystores

#### Updating Keystore Reference

**Steps:**
1. Update `build.gradle` files to reference new keystore
2. Update keystore passwords if changed
3. Test build process with new keystore
4. Verify APK signature after build

**Example Migration:**
```gradle
// Old configuration
storeFile file('../../ahk-release-key.jks')

// New configuration
storeFile file('../../ahk-release-key-v2.jks')
```

#### Compatibility Considerations

- **App Updates:** Same keystore must be used for app updates
- **Play Store:** Google Play requires consistent signing for updates
- **Device Installation:** Signature mismatch prevents installation over existing app

### Security Compliance

#### Industry Standards

- **Key Size:** Minimum 2048-bit RSA keys
- **Validity Period:** Maximum 25-30 years recommended
- **Algorithm:** RSA or ECDSA supported
- **Hash Function:** SHA-256 or higher

#### Audit Trail

- Document all keystore creation and updates
- Maintain logs of APK signing activities
- Track keystore access and usage
- Regular security reviews of signing process

## Testing Procedures

### 1. Phone Number Changes

**Test Steps:**
1. Modify phone number in configuration files
2. Build and install the app
3. Trigger lockscreen to verify phone number display
4. Test payment dialog to ensure correct number appears
5. Verify Firebase payment link contains correct number

**Verification Points:**
- Lockscreen displays new phone number
- Payment dialogs show updated contact information
- Firebase payment URLs contain correct merchant ID

### 2. Payment Link Validation

**Test Steps:**
1. Update `DEFAULT_PAYMENT_LINK` in `FirebaseModule.kt`
2. Test with valid bKash URL
3. Test with valid Nagad URL
4. Test with invalid URL (should fail validation)
5. Verify runtime payment link updates work

**Validation Checklist:**
- ✅ HTTPS protocol required
- ✅ Domain must be `bkash.com` or `nagad.com.bd`
- ✅ URL format validation passes
- ✅ Firebase updates work correctly

### 3. Message Content Testing

**Test Steps:**
1. Modify warning messages in respective files
2. Build and install application
3. Trigger lockscreen to verify message display
4. Test permission screens for warning text
5. Check notification content

**Verification Points:**
- Bengali text renders correctly
- Unicode emojis display properly
- Line breaks and formatting preserved
- Text fits within UI constraints

### 4. Branding Changes

**Test Steps:**
1. Update app name and watermark text
2. Build and install application
3. Check app launcher icon and name
4. Verify main activity watermark
5. Test notification titles

**Verification Points:**
- App name appears correctly in launcher
- Watermark displays on main screen
- Notifications show updated branding
- System dialogs use correct app name

## Impact Analysis

### Phone Number Changes

**High Impact Areas:**
- **Customer Support:** Direct impact on customer contact methods
- **Payment Processing:** Affects payment link generation
- **User Experience:** Changes visible contact information

**Dependencies:**
- Firebase payment link structure
- bKash/Nagad merchant account configuration
- Customer communication channels

### Payment Link Modifications

**Critical Impact Areas:**
- **Revenue Collection:** Direct impact on payment processing
- **Security:** URL validation prevents malicious links
- **Integration:** Must match merchant account settings

**Risk Factors:**
- Invalid URLs break payment functionality
- Incorrect merchant IDs prevent payment processing
- Domain restrictions limit payment provider options

### Warning Message Changes

**Medium Impact Areas:**
- **Legal Compliance:** Warning text may have legal implications
- **User Understanding:** Clear messaging improves compliance
- **Localization:** Bengali text requires proper font support

**Considerations:**
- Legal review may be required for warning text changes
- Translation accuracy important for Bengali content
- Message length affects UI layout

### Branding Updates

**Low-Medium Impact Areas:**
- **Brand Recognition:** Consistent branding across app
- **User Trust:** Professional appearance builds confidence
- **Marketing:** App store presence and user perception

**Technical Considerations:**
- App name changes may affect app store listings
- Notification titles impact system integration
- Watermark text affects main UI layout

## Configuration File Summary

| Parameter Type | File Location | Key Variables |
|----------------|---------------|---------------|
| Phone Numbers | `LockScreen.kt` | Hardcoded `01630138471` |
| Payment Links | `FirebaseModule.kt` | `DEFAULT_PAYMENT_LINK` |
| App Branding | `strings.xml` | `app_name`, `watermark` |
| Warning Messages | `strings.xml`, `PermissionActivity.kt` | `all_permissions_required` |
| Lock Messages | `LockScreen.kt` | Hardcoded Bengali text |
| Notifications | Multiple service files | `.setContentTitle()` calls |

## Best Practices

1. **Always test changes in development environment first**
2. **Backup original configuration files before modifications**
3. **Validate payment URLs before deployment**
4. **Test Bengali text rendering on target devices**
5. **Verify phone number formats match regional standards**
6. **Review legal implications of warning message changes**
7. **Maintain consistent branding across all touchpoints**
8. **Document all configuration changes for future reference**

## Troubleshooting

### Common Issues

1. **Payment Link Validation Fails**
   - Ensure URL uses HTTPS protocol
   - Verify domain is `bkash.com` or `nagad.com.bd`
   - Check for typos in URL structure

2. **Bengali Text Not Displaying**
   - Verify device has Bengali font support
   - Check Unicode encoding in source files
   - Test on multiple device models

3. **Phone Number Not Updating**
   - Confirm changes in all relevant files
   - Clear app cache and reinstall
   - Check Firebase configuration sync

4. **App Name Changes Not Visible**
   - Rebuild and reinstall application
   - Clear launcher cache
   - Check `strings.xml` modifications

For additional support or questions about configuration changes, refer to the development team or create an issue in the project repository.