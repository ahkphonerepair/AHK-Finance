# AHK Finance – Android Device Management & EMI Lock System

[![Version](https://img.shields.io/badge/version-2.0-blue.svg)](https://github.com/ahkphonerepair/AHK-Finance/releases)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](#license)
[![Android](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/backend-Firebase-orange.svg)](https://firebase.google.com)

> 📦 Source Code: `ahk-finance/`  
> 🛡️ All rights reserved to **AHK Phone Repair**

## 📖 Overview

**AHK Finance** is a full-featured Android-based EMI lock and payment management system designed for mobile phone finance providers. The solution enables secure customer device control, real-time payment enforcement, and administrator-level oversight via Firebase services.

---

## 🏗️ Architecture

```
AHK Finance System
├── Admin Application (Management Interface)
│   ├── Device Management
│   ├── Payment Link Updates
│   ├── Real-time Monitoring
│   └── User Administration
│
├── Client Application (Device Enforcement)
│   ├── Device Lock/Unlock Control
│   ├── Payment Status Monitoring
│   ├── Location Tracking
│   └── Anti-tampering Protection
│
└── Backend Services
    ├── Firebase Realtime Database
    ├── Firebase Cloud Messaging
    ├── Firebase Authentication
    └── Cloud Functions
```

## ✨ Key Features

### Admin Application
- **📊 Device Management Dashboard**: Real-time monitoring of all registered devices
- **💳 Payment Link Management**: Dynamic payment URL updates for individual devices
- **🔒 Remote Device Control**: Lock/unlock devices remotely
- **📍 Location Tracking**: GPS-based device location monitoring
- **📱 Device Information**: Hardware specs, IMEI, and system details
- **🔔 Push Notifications**: Send alerts and messages to client devices
- **📈 Analytics & Reporting**: Payment status and device usage analytics

### Client Application
- **🔐 Automatic Device Locking**: Locks device when payment is overdue
- **⚠️ Payment Reminders**: Customizable warning messages in Bengali
- **🛡️ Anti-tampering Protection**: Prevents uninstallation and system modifications
- **📍 Location Services**: Continuous GPS tracking for device recovery
- **🔔 Real-time Notifications**: Instant updates from admin system
- **📞 Support Integration**: Direct access to customer support
- **🎨 Customizable Branding**: Company logo and messaging customization

## 🛠️ Technology Stack

### Frontend (Android)
- **Language**: Kotlin
- **UI Framework**: Android Views with Material Design
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Manual DI
- **Image Loading**: Glide
- **HTTP Client**: Retrofit + OkHttp

### Backend Services
- **Database**: Firebase Realtime Database
- **Authentication**: Firebase Auth
- **Cloud Messaging**: Firebase Cloud Messaging (FCM)
- **Storage**: Firebase Storage
- **Analytics**: Firebase Analytics

### Development Tools
- **IDE**: Android Studio
- **Build System**: Gradle
- **Version Control**: Git
- **Code Signing**: Custom keystore management

## 📚 Documentation

Comprehensive documentation is available to help you get started and configure the AHK Finance system:

### 🔧 Build & Setup
- **[BUILD_GUIDE.md](BUILD_GUIDE.md)** - Complete build script documentation with automated build processes, cross-platform compatibility, and APK management

### ⚙️ Configuration
- **[AHK_Finance_Configuration_Guide.md](AHK_Finance_Configuration_Guide.md)** - Main configuration guide for system setup and customization

### 📋 Release Information
- **[RELEASE_NOTES_v2.0.md](RELEASE_NOTES_v2.0.md)** - Version 2.0 release notes with new features, improvements, and breaking changes

> 💡 **Quick Start**: Begin with the [BUILD_GUIDE.md](BUILD_GUIDE.md) for automated build processes, then refer to the [Configuration Guide](AHK_Finance_Configuration_Guide.md) for system setup.

## 📋 Prerequisites

### Development Environment
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: Java 8 or later
- **Android SDK**: API Level 21 (Android 5.0) minimum
- **Gradle**: 7.0 or later

### Firebase Setup
- Firebase project with Realtime Database enabled
- Firebase Authentication configured
- FCM service keys generated
- `google-services.json` files configured

### Device Requirements
- **Minimum Android Version**: 5.0 (API 21)
- **Target Android Version**: 13 (API 33)
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 100MB available space
- **Permissions**: Device Admin, Location, Storage, Phone

## 🚀 Installation

### 1. Clone Repository
```bash
git clone https://github.com/ahkphonerepair/AHK-Finance.git
cd AHK-Finance
```

### 2. Firebase Configuration

#### Admin App
```bash
# Place your google-services.json in admin-app/app/
cp path/to/admin-google-services.json admin-app/app/google-services.json
```

#### Client App
```bash
# Place your google-services.json in android-client-app/app/
cp path/to/client-google-services.json android-client-app/app/google-services.json
```

### 3. Keystore Setup

```bash
# Ensure keystore files are in project root
ls -la ahk-release-key*.jks

# Verify keystore integrity
keytool -list -keystore ahk-release-key-v2.jks
```

### 4. Build Applications

#### Debug Build
```bash
# Admin App
cd admin-app
./gradlew assembleDebug

# Client App
cd ../android-client-app
./gradlew assembleDebug
```

#### Release Build
```bash
# Admin App
cd admin-app
./gradlew assembleRelease

# Client App
cd ../android-client-app
./gradlew assembleRelease
```

## 📱 Usage

### Admin Application Workflow

1. **Initial Setup**
   - Install admin APK on management device
   - Configure Firebase credentials
   - Set up admin user accounts

2. **Device Registration**
   - Add new devices to the system
   - Configure payment links for each device
   - Set device-specific parameters

3. **Monitoring & Management**
   - Monitor device status in real-time
   - Update payment links as needed
   - Send notifications to client devices
   - Track device locations

### Client Application Workflow

1. **Device Setup**
   - Install client APK on target device
   - Grant required permissions
   - Enable device administrator privileges

2. **Registration**
   - Device automatically registers with backend
   - Receives initial configuration from admin
   - Starts monitoring services

3. **Operation**
   - Monitors payment status continuously
   - Displays warnings when payment is due
   - Locks device if payment is overdue
   - Reports location and status to admin

## ⚙️ Configuration

### Environment Variables

```bash
# Keystore Configuration
export KEYSTORE_PATH="/path/to/ahk-release-key-v2.jks"
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="ahk_release_key"
export KEY_PASSWORD="your_key_password"

# Firebase Configuration
export FIREBASE_PROJECT_ID="your-project-id"
export FIREBASE_API_KEY="your-api-key"
```

### Customization Options

Refer to the [AHK Finance Configuration Guide](AHK_Finance_Configuration_Guide.md) for detailed configuration instructions including:

- Support phone number updates
- Payment link configuration
- Warning message customization
- App branding modifications
- APK signature management

## 🧪 Testing

### Unit Testing
```bash
# Run unit tests for admin app
cd admin-app
./gradlew test

# Run unit tests for client app
cd android-client-app
./gradlew test
```

### Integration Testing
```bash
# Run instrumented tests
./gradlew connectedAndroidTest
```

### Manual Testing Checklist

- [ ] Device registration and authentication
- [ ] Payment link updates and validation
- [ ] Device lock/unlock functionality
- [ ] Location tracking accuracy
- [ ] Push notification delivery
- [ ] Anti-tampering protection
- [ ] Support contact integration

## 📊 Monitoring & Analytics

### Firebase Analytics Events
- Device registration events
- Payment status changes
- Lock/unlock actions
- Location updates
- App usage metrics

### Performance Monitoring
- App startup time
- Network request latency
- Battery usage optimization
- Memory consumption tracking

## 🔒 Security Features

### Client App Protection
- **Anti-uninstall**: Prevents unauthorized app removal
- **Root Detection**: Detects and responds to rooted devices
- **Tamper Protection**: Monitors system modifications
- **Secure Communication**: Encrypted data transmission

### Data Security
- **Firebase Security Rules**: Restricts database access
- **API Key Protection**: Secure key management
- **User Authentication**: Multi-factor authentication support
- **Data Encryption**: End-to-end encryption for sensitive data

## 🚨 Troubleshooting

### Common Issues

#### Build Errors
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Check dependency conflicts
./gradlew dependencies
```

#### Firebase Connection Issues
```bash
# Verify google-services.json placement
# Check Firebase project configuration
# Validate API keys and permissions
```

#### Device Admin Issues
```bash
# Ensure device admin permissions are granted
# Check for conflicting device policies
# Verify app is not in battery optimization whitelist
```

### Debug Commands

```bash
# Check APK signature
apksigner verify --verbose app-release.apk

# View device logs
adb logcat | grep "AHK"

# Check app permissions
adb shell dumpsys package com.emi.ahkfinance
```

## 📈 Performance Optimization

### Battery Optimization
- Background service optimization
- Efficient location tracking
- Smart notification scheduling
- Doze mode compatibility

### Network Optimization
- Request batching
- Offline capability
- Data compression
- Connection pooling

## 🔄 Version History

### Version 2.0 (Current)
- Enhanced device management interface
- Improved payment link system
- Better security features
- Performance optimizations
- Updated Firebase integration

### Version 1.0
- Initial release
- Basic device management
- Payment tracking
- Location services

See [RELEASE_NOTES_v2.0.md](RELEASE_NOTES_v2.0.md) for detailed changelog.

## 🤝 Contributing

### Development Guidelines
1. Follow Android development best practices
2. Maintain code documentation
3. Write unit tests for new features
4. Follow Git commit message conventions
5. Ensure security compliance

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Maintain consistent indentation

## 📞 Support

### Technical Support
- **Email**: support@ahkfinance.com
- **Phone**: +880-1234-567890
- **Documentation**: [Configuration Guide](.trae/documents/AHK_Finance_Configuration_Guide.md)

### Business Inquiries
- **Email**: business@ahkfinance.com
- **Phone**: +880-1234-567891

## 📄 License

This project is proprietary software owned by AHK Finance. All rights reserved.

**Restrictions:**
- No unauthorized copying or distribution
- No reverse engineering
- Commercial use requires explicit permission
- Source code access is limited to authorized personnel

## 🙏 Acknowledgments

- Firebase team for backend services
- Android development community
- Material Design guidelines
- Open source libraries used in the project

---

**© 2024 AHK Finance. All rights reserved.**

*For technical documentation and configuration details, please refer to the [AHK Finance Configuration Guide](AHK_Finance_Configuration_Guide.md).*
