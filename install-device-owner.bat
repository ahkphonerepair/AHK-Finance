@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM AHK Finance - Automated Device Owner Installation Script
REM ============================================================================
REM This script automates the process of setting up AHK Finance as Device Owner
REM using ADB commands. It handles ADB installation, device preparation, and
REM device owner configuration.
REM ============================================================================

title AHK Finance - Device Owner Installation
color 0A

echo.
echo ============================================================================
echo                    AHK Finance - Device Owner Setup
echo ============================================================================
echo.
echo This script will help you set up AHK Finance as Device Owner on Android devices.
echo Please ensure you have followed the prerequisites before continuing.
echo.
echo Prerequisites:
echo  1. Device must be factory reset or have no Google accounts
echo  2. USB Debugging must be enabled
echo  3. AHK Finance app must be installed on the device
echo  4. Device must be connected via USB
echo.
pause

REM Check if running as administrator
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo [ERROR] This script requires administrator privileges.
    echo Please right-click and select "Run as administrator"
    echo.
    pause
    exit /b 1
)

REM Set variables
set "ADB_DIR=%~dp0"
set "ADB_EXE=%ADB_DIR%adb.exe"
set "PACKAGE_NAME=com.emi.ahkfinance"
set "DEVICE_ADMIN_RECEIVER=com.emi.ahkfinance/.DeviceAdminReceiver"
set "APK_FILE=%ADB_DIR%ahk-finance.apk"

echo.
echo ============================================================================
echo                           Step 1: ADB Setup
echo ============================================================================
echo.

REM Check if ADB exists in current directory
if exist "%ADB_EXE%" (
    echo [INFO] ADB found in current directory: %ADB_EXE%
) else (
    echo [INFO] ADB not found in current directory. Checking system PATH...
    
    REM Check if ADB is in system PATH
    adb version >nul 2>&1
    if !errorLevel! equ 0 (
        echo [INFO] ADB found in system PATH
        set "ADB_EXE=adb"
    ) else (
        echo [ERROR] ADB not found. Please ensure adb.exe is in the same directory as this script
        echo         or install Android SDK Platform Tools and add to PATH.
        echo.
        echo Download from: https://developer.android.com/studio/releases/platform-tools
        echo.
        pause
        exit /b 1
    )
)

REM Test ADB
echo [INFO] Testing ADB connection...
"%ADB_EXE%" version
if %errorLevel% neq 0 (
    echo [ERROR] ADB is not working properly
    pause
    exit /b 1
)

echo.
echo ============================================================================
echo                        Step 2: Device Detection
echo ============================================================================
echo.

echo [INFO] Scanning for connected devices...
"%ADB_EXE%" devices

REM Check if any devices are connected
for /f "skip=1 tokens=1" %%i in ('"%ADB_EXE%" devices') do (
    if not "%%i"=="" (
        set "DEVICE_FOUND=1"
        set "DEVICE_ID=%%i"
        goto :device_found
    )
)

echo.
echo [ERROR] No devices found. Please check:
echo  1. Device is connected via USB
echo  2. USB Debugging is enabled
echo  3. USB drivers are installed
echo  4. Device is unlocked and USB debugging is authorized
echo.
pause
exit /b 1

:device_found
echo.
echo [SUCCESS] Device found: %DEVICE_ID%
echo.

REM Get device information
echo [INFO] Getting device information...
for /f "tokens=*" %%i in ('"%ADB_EXE%" shell getprop ro.product.model') do set "DEVICE_MODEL=%%i"
for /f "tokens=*" %%i in ('"%ADB_EXE%" shell getprop ro.build.version.release') do set "ANDROID_VERSION=%%i"
for /f "tokens=*" %%i in ('"%ADB_EXE%" shell getprop ro.product.manufacturer') do set "DEVICE_MANUFACTURER=%%i"

echo Device Model: %DEVICE_MANUFACTURER% %DEVICE_MODEL%
echo Android Version: %ANDROID_VERSION%
echo.

echo.
echo ============================================================================
echo                      Step 3: Device Preparation
echo ============================================================================
echo.

echo [INFO] Checking device preparation requirements...

REM Check if AHK Finance is installed
echo [INFO] Checking if AHK Finance is installed...
"%ADB_EXE%" shell pm list packages | findstr "%PACKAGE_NAME%" >nul
if %errorLevel% neq 0 (
    echo [WARNING] AHK Finance app is not installed on the device.
    echo.
    if exist "%APK_FILE%" (
        echo [INFO] Found APK file. Installing AHK Finance...
        "%ADB_EXE%" install "%APK_FILE%"
        if !errorLevel! neq 0 (
            echo [ERROR] Failed to install AHK Finance APK
            pause
            exit /b 1
        )
        echo [SUCCESS] AHK Finance installed successfully
    ) else (
        echo [ERROR] AHK Finance APK not found: %APK_FILE%
        echo Please install the app manually or place the APK file in the script directory.
        pause
        exit /b 1
    )
) else (
    echo [SUCCESS] AHK Finance is already installed
)

REM Check for existing device owner
echo [INFO] Checking for existing device owner...
for /f "tokens=*" %%i in ('"%ADB_EXE%" shell dpm list-owners 2^>nul') do (
    echo %%i | findstr "Device owner" >nul
    if !errorLevel! equ 0 (
        echo [WARNING] Device already has a device owner:
        echo %%i
        echo.
        echo Do you want to remove the existing device owner? (y/n)
        set /p "REMOVE_OWNER="
        if /i "!REMOVE_OWNER!"=="y" (
            echo [INFO] Attempting to remove existing device owner...
            "%ADB_EXE%" shell dpm remove-active-admin %%i
            if !errorLevel! neq 0 (
                echo [ERROR] Failed to remove existing device owner
                echo You may need to factory reset the device
                pause
                exit /b 1
            )
        ) else (
            echo [INFO] Keeping existing device owner. Exiting...
            pause
            exit /b 0
        )
    )
)

REM Check for Google accounts
echo [INFO] Checking for Google accounts...
"%ADB_EXE%" shell dumpsys account | findstr "com.google" >nul
if %errorLevel% equ 0 (
    echo [WARNING] Google accounts detected on device.
    echo Device Owner setup may fail with Google accounts present.
    echo.
    echo Recommendations:
    echo  1. Remove all Google accounts from Settings ^> Accounts
    echo  2. Disable Find My Device in Settings ^> Security
    echo  3. Or perform a factory reset
    echo.
    echo Do you want to continue anyway? (y/n)
    set /p "CONTINUE_WITH_ACCOUNTS="
    if /i "!CONTINUE_WITH_ACCOUNTS!" neq "y" (
        echo [INFO] Please remove Google accounts and run the script again.
        pause
        exit /b 0
    )
)

echo.
echo ============================================================================
echo                     Step 4: Device Owner Setup
echo ============================================================================
echo.

echo [INFO] Setting AHK Finance as Device Owner...
echo.
echo Command: dpm set-device-owner %DEVICE_ADMIN_RECEIVER%
echo.

"%ADB_EXE%" shell dpm set-device-owner "%DEVICE_ADMIN_RECEIVER%"
set "SETUP_RESULT=%errorLevel%"

if %SETUP_RESULT% equ 0 (
    echo.
    echo [SUCCESS] AHK Finance has been successfully set as Device Owner!
    echo.
) else (
    echo.
    echo [ERROR] Failed to set Device Owner. Common causes:
    echo  1. Google accounts are still present on the device
    echo  2. Find My Device is enabled
    echo  3. Another device owner is already set
    echo  4. Device needs to be factory reset
    echo.
    echo Troubleshooting steps:
    echo  1. Factory reset the device completely
    echo  2. Skip Google account setup during initial setup
    echo  3. Enable USB debugging
    echo  4. Run this script again
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================================================
echo                        Step 5: Verification
echo ============================================================================
echo.

echo [INFO] Verifying Device Owner setup...

REM Verify device owner status
"%ADB_EXE%" shell dpm list-owners
echo.

REM Check if our app is the device owner
"%ADB_EXE%" shell dpm list-owners | findstr "%DEVICE_ADMIN_RECEIVER%" >nul
if %errorLevel% equ 0 (
    echo [SUCCESS] Verification passed! AHK Finance is confirmed as Device Owner.
) else (
    echo [ERROR] Verification failed! AHK Finance is not set as Device Owner.
    pause
    exit /b 1
)

echo.
echo ============================================================================
echo                      Step 6: Enhanced Features
echo ============================================================================
echo.

echo [INFO] Configuring enhanced Device Owner features...

REM Enable kiosk mode packages
echo [INFO] Setting up kiosk mode...
"%ADB_EXE%" shell dpm set-lock-task-packages "%DEVICE_ADMIN_RECEIVER%" "%PACKAGE_NAME%"

REM Set user restrictions for enhanced security
echo [INFO] Applying security restrictions...
"%ADB_EXE%" shell dpm add-user-restriction "%DEVICE_ADMIN_RECEIVER%" no_factory_reset
"%ADB_EXE%" shell dpm add-user-restriction "%DEVICE_ADMIN_RECEIVER%" no_add_user
"%ADB_EXE%" shell dpm add-user-restriction "%DEVICE_ADMIN_RECEIVER%" no_install_unknown_sources

REM Disable status bar (optional)
echo.
echo Do you want to disable the status bar for enhanced kiosk mode? (y/n)
set /p "DISABLE_STATUS_BAR="
if /i "!DISABLE_STATUS_BAR!"=="y" (
    "%ADB_EXE%" shell dpm add-user-restriction "%DEVICE_ADMIN_RECEIVER%" no_status_bar
    echo [INFO] Status bar disabled
)

echo.
echo ============================================================================
echo                           Setup Complete!
echo ============================================================================
echo.
echo [SUCCESS] AHK Finance Device Owner setup completed successfully!
echo.
echo Device Information:
echo  - Model: %DEVICE_MANUFACTURER% %DEVICE_MODEL%
echo  - Android: %ANDROID_VERSION%
echo  - Device Owner: AHK Finance
echo  - Kiosk Mode: Enabled
echo  - Security Restrictions: Applied
echo.
echo Next Steps:
echo  1. Launch the AHK Finance app on the device
echo  2. Complete the registration process
echo  3. Test device locking functionality
echo  4. Verify kiosk mode operation
echo.
echo Enhanced Features Available:
echo  - Remote device locking/unlocking
echo  - Kiosk mode activation
echo  - Prevent app uninstallation
echo  - System settings restrictions
echo  - Factory reset protection
echo.
echo For troubleshooting and support, refer to the documentation.
echo.

REM Create verification report
echo [INFO] Creating verification report...
set "REPORT_FILE=%~dp0device-owner-setup-report.txt"
echo AHK Finance Device Owner Setup Report > "%REPORT_FILE%"
echo ========================================= >> "%REPORT_FILE%"
echo Date: %date% %time% >> "%REPORT_FILE%"
echo Device Model: %DEVICE_MANUFACTURER% %DEVICE_MODEL% >> "%REPORT_FILE%"
echo Android Version: %ANDROID_VERSION% >> "%REPORT_FILE%"
echo Device ID: %DEVICE_ID% >> "%REPORT_FILE%"
echo Setup Result: SUCCESS >> "%REPORT_FILE%"
echo Device Owner: %DEVICE_ADMIN_RECEIVER% >> "%REPORT_FILE%"
echo ========================================= >> "%REPORT_FILE%"
"%ADB_EXE%" shell dpm list-owners >> "%REPORT_FILE%"
echo. >> "%REPORT_FILE%"
echo Verification report saved to: %REPORT_FILE%

echo.
echo Press any key to exit...
pause >nul
exit /b 0

REM Error handling function
:error_exit
echo.
echo [ERROR] An unexpected error occurred during setup.
echo Please check the error messages above and try again.
echo.
echo For support, contact AHK Phone Repair technical team.
echo.
pause
exit /b 1