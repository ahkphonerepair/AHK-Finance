@echo off
echo Building AHK Admin App (Release Version)...
echo ==========================================

:: Clean previous builds
echo Cleaning previous builds...
call gradlew clean

:: Build release version
echo Building release APK...
call gradlew app:assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================
    echo BUILD SUCCESSFUL!
    echo ==========================================
    echo Release APK location:
    echo app\build\outputs\apk\release\app-release.apk
    echo.
    pause
) else (
    echo.
    echo ==========================================
    echo BUILD FAILED!
    echo ==========================================
    echo Check the error messages above.
    echo.
    pause
)