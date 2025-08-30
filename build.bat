@echo off
setlocal enabledelayedexpansion

:: ============================================================================
:: AHK Finance Professional Build Script v2.0
:: Automated build system for Admin and Client Android applications
:: ============================================================================

:: Configuration Variables
set "SCRIPT_VERSION=2.0"
set "PROJECT_NAME=AHK Finance"
set "BUILD_DATE=%date% %time%"
set "LOG_DIR=build-logs"
set "LOG_FILE=%LOG_DIR%\build-%date:~-4,4%%date:~-10,2%%date:~-7,2%-%time:~0,2%%time:~3,2%%time:~6,2%.log"
set "ADMIN_APP_DIR=admin-app"
set "CLIENT_APP_DIR=android-client-app"
set "BUILD_MODE=release"
set "CLEAN_BUILD=true"
set "BUILD_ADMIN=true"
set "BUILD_CLIENT=true"
set "TOTAL_STEPS=0"
set "CURRENT_STEP=0"
set "ERROR_COUNT=0"

:: Color codes for enhanced UI (removed for compatibility)
set "COLOR_HEADER=0"
set "COLOR_SUCCESS=0"
set "COLOR_ERROR=0"
set "COLOR_WARNING=0"
set "COLOR_INFO=0"
set "COLOR_PROGRESS=0"
set "COLOR_RESET=0"

:: Main entry point
call :parse_args %*
goto :eof

:: Parse command line arguments
:parse_args
if "%~1"=="" goto :start_build
if /i "%~1"=="--help" goto :show_help
if /i "%~1"=="-h" goto :show_help
if /i "%~1"=="--debug" set "BUILD_MODE=debug" & shift & goto :parse_args
if /i "%~1"=="--release" set "BUILD_MODE=release" & shift & goto :parse_args
if /i "%~1"=="--no-clean" set "CLEAN_BUILD=false" & shift & goto :parse_args
if /i "%~1"=="--admin-only" set "BUILD_CLIENT=false" & shift & goto :parse_args
if /i "%~1"=="--client-only" set "BUILD_ADMIN=false" & shift & goto :parse_args
if /i "%~1"=="--verbose" set "VERBOSE=true" & shift & goto :parse_args
shift
goto :parse_args

:show_help
echo.
echo ================================================================================
echo                        AHK Finance Build Script v%SCRIPT_VERSION%
echo ================================================================================
echo.
echo USAGE: build.bat [OPTIONS]
echo.
echo OPTIONS:
echo   --help, -h        Show this help message
echo   --debug           Build in debug mode (default: release)
echo   --release         Build in release mode
echo   --no-clean        Skip clean build step
echo   --admin-only      Build only the admin application
echo   --client-only     Build only the client application
echo   --verbose         Enable verbose logging
echo.
echo EXAMPLES:
echo   build.bat                    Build both apps in release mode
echo   build.bat --debug            Build both apps in debug mode
echo   build.bat --admin-only       Build only admin app
echo   build.bat --client-only      Build only client app
echo   build.bat --no-clean         Skip clean step for faster builds
echo.
pause
exit /b 0

:start_build
:: Initialize logging
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
echo Build started at %BUILD_DATE% > "%LOG_FILE%"
echo Command line: %* >> "%LOG_FILE%"
echo. >> "%LOG_FILE%"

:: Calculate total steps
set /a TOTAL_STEPS=2
if "%CLEAN_BUILD%"=="true" set /a TOTAL_STEPS+=2
if "%BUILD_ADMIN%"=="true" set /a TOTAL_STEPS+=2
if "%BUILD_CLIENT%"=="true" set /a TOTAL_STEPS+=2

:: Display header
cls
echo.
echo ================================================================================
echo                        AHK EMI Build Script v%SCRIPT_VERSION%
echo                     Professional Android Build System
echo ================================================================================
echo.
echo Build Configuration:
echo   - Build Mode: %BUILD_MODE%
echo   - Clean Build: %CLEAN_BUILD%
echo   - Build Admin App: %BUILD_ADMIN%
echo   - Build Client App: %BUILD_CLIENT%
echo   - Log File: %LOG_FILE%
echo.
echo Starting build process...
echo.

:: Step 1: Environment validation
call :show_progress "Validating build environment"
echo [%date% %time%] Validating build environment >> "%LOG_FILE%"

:: Check if Java is available
java -version >nul 2>&1
if !errorlevel! neq 0 (
    call :show_error "Java not found. Please install Java JDK 8 or higher."
    echo [ERROR] Java not found >> "%LOG_FILE%"
    goto :build_failed
)

:: Check if Android SDK is configured
if not exist "%ADMIN_APP_DIR%\gradlew.bat" (
    call :show_error "Gradle wrapper not found in admin-app directory."
    echo [ERROR] Gradle wrapper not found in admin-app >> "%LOG_FILE%"
    goto :build_failed
)

if not exist "%CLIENT_APP_DIR%\gradlew.bat" (
    call :show_error "Gradle wrapper not found in android-client-app directory."
    echo [ERROR] Gradle wrapper not found in android-client-app >> "%LOG_FILE%"
    goto :build_failed
)

:: Check keystore files
if not exist "ahk-release-key.jks" (
    call :show_warning "Admin app keystore (ahk-release-key.jks) not found."
    echo [WARNING] Admin app keystore not found >> "%LOG_FILE%"
)

if not exist "ahk-release-key-v2.jks" (
    call :show_warning "Client app keystore (ahk-release-key-v2.jks) not found."
    echo [WARNING] Client app keystore not found >> "%LOG_FILE%"
)

call :show_success "Environment validation completed"
echo [SUCCESS] Environment validation completed >> "%LOG_FILE%"

:: Step 2: Clean builds (if requested)
if "%CLEAN_BUILD%"=="true" (
    if "%BUILD_ADMIN%"=="true" (
        call :show_progress "Cleaning admin app build cache"
        echo [%date% %time%] Cleaning admin app >> "%LOG_FILE%"
        cd "%ADMIN_APP_DIR%"
        call gradlew.bat clean >> "..\%LOG_FILE%" 2>&1
        if !errorlevel! neq 0 (
            call :show_error "Failed to clean admin app"
            echo [ERROR] Admin app clean failed >> "..\%LOG_FILE%"
            cd ..
            set /a ERROR_COUNT+=1
        ) else (
            call :show_success "Admin app cleaned successfully"
            echo [SUCCESS] Admin app cleaned >> "..\%LOG_FILE%"
        )
        cd ..
    )
    
    if "%BUILD_CLIENT%"=="true" (
        call :show_progress "Cleaning client app build cache"
        echo [%date% %time%] Cleaning client app >> "%LOG_FILE%"
        cd "%CLIENT_APP_DIR%"
        call gradlew.bat clean >> "..\%LOG_FILE%" 2>&1
        if !errorlevel! neq 0 (
            call :show_error "Failed to clean client app"
            echo [ERROR] Client app clean failed >> "..\%LOG_FILE%"
            cd ..
            set /a ERROR_COUNT+=1
        ) else (
            call :show_success "Client app cleaned successfully"
            echo [SUCCESS] Client app cleaned >> "..\%LOG_FILE%"
        )
        cd ..
    )
)

:: Step 3: Build admin app
if "%BUILD_ADMIN%"=="true" (
    call :show_progress "Building admin application (%BUILD_MODE% mode)"
    echo [%date% %time%] Building admin app in %BUILD_MODE% mode >> "%LOG_FILE%"
    cd "%ADMIN_APP_DIR%"
    
    if "%BUILD_MODE%"=="debug" (
        call gradlew.bat assembleDebug >> "..\%LOG_FILE%" 2>&1
    ) else (
        call gradlew.bat assembleRelease >> "..\%LOG_FILE%" 2>&1
    )
    
    if !errorlevel! neq 0 (
        call :show_error "Admin app build failed"
        echo [ERROR] Admin app build failed >> "..\%LOG_FILE%"
        cd ..
        set /a ERROR_COUNT+=1
    ) else (
        call :show_success "Admin app built successfully"
        echo [SUCCESS] Admin app built successfully >> "..\%LOG_FILE%"
        
        :: Display APK location
        if "%BUILD_MODE%"=="debug" (
            set "ADMIN_APK=app\build\outputs\apk\debug\app-debug.apk"
        ) else (
            set "ADMIN_APK=app\build\outputs\apk\release\app-release.apk"
        )
        
        if exist "!ADMIN_APK!" (
            call :show_info "Admin APK: %ADMIN_APP_DIR%\!ADMIN_APK!"
            echo [INFO] Admin APK location: !ADMIN_APK! >> "..\%LOG_FILE%"
            
            :: Copy and rename APK to root directory
            if "%BUILD_MODE%"=="debug" (
                call :copy_apk_to_root "!ADMIN_APK!" "debug.apk" "admin"
            ) else (
                call :copy_apk_to_root "!ADMIN_APK!" "admin-release.apk" "admin"
            )
        )
    )
    cd ..
)

:: Step 4: Build client app
if "%BUILD_CLIENT%"=="true" (
    call :show_progress "Building client application (%BUILD_MODE% mode)"
    echo [%date% %time%] Building client app in %BUILD_MODE% mode >> "%LOG_FILE%"
    cd "%CLIENT_APP_DIR%"
    
    if "%BUILD_MODE%"=="debug" (
        call gradlew.bat assembleDebug >> "..\%LOG_FILE%" 2>&1
    ) else (
        call gradlew.bat assembleRelease >> "..\%LOG_FILE%" 2>&1
    )
    
    if !errorlevel! neq 0 (
        call :show_error "Client app build failed"
        echo [ERROR] Client app build failed >> "..\%LOG_FILE%"
        cd ..
        set /a ERROR_COUNT+=1
    ) else (
        call :show_success "Client app built successfully"
        echo [SUCCESS] Client app built successfully >> "..\%LOG_FILE%"
        
        :: Display APK location
        if "%BUILD_MODE%"=="debug" (
            set "CLIENT_APK=app\build\outputs\apk\debug\app-debug.apk"
        ) else (
            set "CLIENT_APK=app\build\outputs\apk\release\app-release.apk"
        )
        
        if exist "!CLIENT_APK!" (
            call :show_info "Client APK: %CLIENT_APP_DIR%\!CLIENT_APK!"
            echo [INFO] Client APK location: !CLIENT_APK! >> "..\%LOG_FILE%"
            
            :: Copy and rename APK to root directory
            if "%BUILD_MODE%"=="debug" (
                call :copy_apk_to_root "!CLIENT_APK!" "debug.apk" "client"
            ) else (
                call :copy_apk_to_root "!CLIENT_APK!" "client-release.apk" "client"
            )
        )
    )
    cd ..
)

:: Build completion summary
echo.
echo ================================================================================
if !ERROR_COUNT! equ 0 (
    echo                            BUILD COMPLETED SUCCESSFULLY
    echo ================================================================================
    echo.
    echo [SUCCESS] All builds completed without errors
    echo [SUCCESS] Build mode: %BUILD_MODE%
    echo [SUCCESS] Log file: %LOG_FILE%
    echo.
    if "%BUILD_ADMIN%"=="true" echo [SUCCESS] Admin APK: %ADMIN_APP_DIR%\!ADMIN_APK!
    if "%BUILD_CLIENT%"=="true" echo [SUCCESS] Client APK: %CLIENT_APP_DIR%\!CLIENT_APK!
    echo.
    echo [%date% %time%] Build completed successfully >> "%LOG_FILE%"
    goto :build_success
) else (
    echo                              BUILD FAILED
    echo ================================================================================
    echo.
    echo [ERROR] Build completed with !ERROR_COUNT! error(s)
    echo [ERROR] Check log file for details: %LOG_FILE%
    echo.
    echo [%date% %time%] Build failed with !ERROR_COUNT! errors >> "%LOG_FILE%"
    goto :build_failed
)

:build_success
echo Build completed at %date% %time% >> "%LOG_FILE%"
echo.
echo Press any key to exit...
pause >nul
exit /b 0

:build_failed
echo Build failed at %date% %time% >> "%LOG_FILE%"
echo.
echo Press any key to exit...
pause >nul
exit /b 1

:: ============================================================================
:: Utility Functions
:: ============================================================================

:show_progress
set /a CURRENT_STEP+=1
echo [%CURRENT_STEP%/%TOTAL_STEPS%] %~1
goto :eof

:show_success
echo [SUCCESS] %~1
goto :eof

:show_error
echo [ERROR] %~1
goto :eof

:show_warning
echo [WARNING] %~1
goto :eof

:show_info
echo [INFO] %~1
goto :eof

:copy_apk_to_root
:: Function to copy and rename APK files to root directory
:: Parameters: %1 = source APK path, %2 = target filename, %3 = app type
set "SOURCE_APK=%~1"
set "TARGET_NAME=%~2"
set "APP_TYPE=%~3"
set "ROOT_APK=..\%TARGET_NAME%"

echo [%date% %time%] Copying %APP_TYPE% APK to root directory >> "..\%LOG_FILE%"
call :show_progress "Copying %APP_TYPE% APK to root directory as %TARGET_NAME%"

if exist "%SOURCE_APK%" (
    copy "%SOURCE_APK%" "%ROOT_APK%" >nul 2>&1
    if !errorlevel! equ 0 (
        call :show_success "APK copied successfully: %TARGET_NAME%"
        echo [SUCCESS] APK copied to root: %TARGET_NAME% >> "..\%LOG_FILE%"
    ) else (
        call :show_error "Failed to copy APK to root directory"
        echo [ERROR] Failed to copy APK: %SOURCE_APK% to %ROOT_APK% >> "..\%LOG_FILE%"
        set /a ERROR_COUNT+=1
    )
) else (
    call :show_error "Source APK not found: %SOURCE_APK%"
    echo [ERROR] Source APK not found: %SOURCE_APK% >> "..\%LOG_FILE%"
    set /a ERROR_COUNT+=1
)
goto :eof

:: ============================================================================
:: End of Script
:: ============================================================================