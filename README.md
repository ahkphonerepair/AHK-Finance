# AHK Finance – Android Device Management & EMI Lock System

> 📦 Source Code: `ahk-finance/`  
> 🛡️ All rights reserved to **AHK Phone Repair**

## 📖 Overview

**AHK Finance** is a full-featured Android-based EMI lock and payment management system designed for mobile phone finance providers. The solution enables secure customer device control, real-time payment enforcement, and administrator-level oversight via Firebase services.

---

## 📂 Repository Structure

ahk-phone-repair/ 

├── ahk-finance/           # → Full source code (Client + Admin app) 
├── README.md              # → This file ├── LICENSE                # → Legal license & usage terms 
├── documentation/         # → Technical setup, device owner guide, ADB usage └── demonstration/             # → Screenshots, videos, usage flows

---

## 🔧 Components

### 1. Client App (User Device)
- Bangla Material3 UI
- Anti-uninstall protection
- Device Admin & Device Owner support
- Auto-lock on due date or remote command
- Firebase FCM-based remote management
- Offline lock functionality

### 2. Admin App (Internal Use)
- Firebase Authentication (email/password)
- Live device status, lock/unlock control
- Due amount & date editing
- Bulk operations
- Admin-only access with custom Firebase claims

### 3. Firebase Backend
- Firestore for secure data storage
- Cloud Messaging for remote actions
- Authentication for admin access control
- Role-based Firestore security rules

---

## 🚀 Deployment & Setup

### Prerequisites
- Android Studio (latest)
- Android SDK 23+
- Firebase project (Firestore, FCM, Auth)
- `google-services.json` for both apps

### Setup Instructions
```bash
git clone https://github.com/ahkphonerepair/AHK-Finance.git
cd ahkphonerepair/AHK-Finance/

1. Add google-services.json to both app/ modules


2. Sync Gradle and resolve dependencies


3. Build and install on real devices


4. Enable Device Admin on first launch


5. Optionally set Device Owner using ADB:



adb shell dpm set-device-owner com.emi.ahkfinance/.DeviceAdminReceiver


---

🔐 Security Model

Role	Permissions

Client Device	Register, update self, sync status
Admin User	View & control all devices
Admin with Claims	Edit payment fields, delete, full access


Data encrypted using EncryptedSharedPreferences

Firebase rules enforce strict role-based access



---

📱 Zero-touch Enrollment

AHK Finance supports provisioning via Google Zero-touch Enrollment with integration into the Android Management API, enabling:

Fully automated enrollment of financed devices

Remote provisioning without manual setup

Pre-configured Device Owner and locked policy


📧 For provisioning or enrollment support:
support+ahkphonerepair@gmail.com


---

✅ Deployment Checklist

🔐 App signed with release key

🔒 Firebase rules deployed

✅ Admin accounts set with custom claims

🧪 Tested on target device models



---

📄 License

All files, designs, and source code are © by AHK Phone Repair.
Redistribution, modification, or rebranding is strictly prohibited without written permission.


---

📬 Contact

For technical support, business inquiries, or provisioning help:

AHK Phone Repair
📧 support+ahkphonerepair@gmail.com


---

> 🛠️ Development and maintenance by AHK Phone Repair
