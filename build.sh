#!/bin/bash

# ============================================================================
# AHK Finance Professional Build Script v2.0
# Automated build system for Admin and Client Android applications
# ============================================================================

# Configuration Variables
SCRIPT_VERSION="2.0"
PROJECT_NAME="AHK Finance"
BUILD_DATE=$(date '+%Y-%m-%d %H:%M:%S')
LOG_DIR="build-logs"
LOG_FILE="$LOG_DIR/build-$(date '+%Y%m%d-%H%M%S').log"
ADMIN_APP_DIR="admin-app"
CLIENT_APP_DIR="android-client-app"
BUILD_MODE="release"
CLEAN_BUILD=true
BUILD_ADMIN=true
BUILD_CLIENT=true
TOTAL_STEPS=0
CURRENT_STEP=0
ERROR_COUNT=0
VERBOSE=false

# Color codes for enhanced UI
COLOR_HEADER='\033[96m'
COLOR_SUCCESS='\033[92m'
COLOR_ERROR='\033[91m'
COLOR_WARNING='\033[93m'
COLOR_INFO='\033[94m'
COLOR_PROGRESS='\033[95m'
COLOR_RESET='\033[0m'

# Function to show help
show_help() {
    echo -e "${COLOR_HEADER}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                        AHK Finance Build Script v$SCRIPT_VERSION                        ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${COLOR_RESET}"
    echo
    echo "USAGE: ./build.sh [OPTIONS]"
    echo
    echo "OPTIONS:"
    echo "  --help, -h        Show this help message"
    echo "  --debug           Build in debug mode (default: release)"
    echo "  --release         Build in release mode"
    echo "  --no-clean        Skip clean build step"
    echo "  --admin-only      Build only the admin application"
    echo "  --client-only     Build only the client application"
    echo "  --verbose         Enable verbose logging"
    echo
    echo "EXAMPLES:"
    echo "  ./build.sh                    Build both apps in release mode"
    echo "  ./build.sh --debug            Build both apps in debug mode"
    echo "  ./build.sh --admin-only       Build only admin app"
    echo "  ./build.sh --client-only      Build only client app"
    echo "  ./build.sh --no-clean         Skip clean step for faster builds"
    echo
    exit 0
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-h)
            show_help
            ;;
        --debug)
            BUILD_MODE="debug"
            shift
            ;;
        --release)
            BUILD_MODE="release"
            shift
            ;;
        --no-clean)
            CLEAN_BUILD=false
            shift
            ;;
        --admin-only)
            BUILD_CLIENT=false
            shift
            ;;
        --client-only)
            BUILD_ADMIN=false
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            ;;
    esac
done

# Utility functions
show_progress() {
    ((CURRENT_STEP++))
    echo -e "${COLOR_PROGRESS}[$CURRENT_STEP/$TOTAL_STEPS] $1${COLOR_RESET}"
}

show_success() {
    echo -e "${COLOR_SUCCESS}✓ $1${COLOR_RESET}"
}

show_error() {
    echo -e "${COLOR_ERROR}✗ $1${COLOR_RESET}"
}

show_warning() {
    echo -e "${COLOR_WARNING}⚠ $1${COLOR_RESET}"
}

show_info() {
    echo -e "${COLOR_INFO}ℹ $1${COLOR_RESET}"
}

# Initialize logging
mkdir -p "$LOG_DIR"
echo "Build started at $BUILD_DATE" > "$LOG_FILE"
echo "Command line: $0 $*" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

# Calculate total steps
TOTAL_STEPS=2
if [ "$CLEAN_BUILD" = true ]; then
    if [ "$BUILD_ADMIN" = true ]; then ((TOTAL_STEPS++)); fi
    if [ "$BUILD_CLIENT" = true ]; then ((TOTAL_STEPS++)); fi
fi
if [ "$BUILD_ADMIN" = true ]; then ((TOTAL_STEPS++)); fi
if [ "$BUILD_CLIENT" = true ]; then ((TOTAL_STEPS++)); fi

# Display header
clear
echo -e "${COLOR_HEADER}"
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                        AHK EMI Build Script v$SCRIPT_VERSION                        ║"
echo "║                     Professional Android Build System                        ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo -e "${COLOR_RESET}"
echo
echo "Build Configuration:"
echo "  • Build Mode: $BUILD_MODE"
echo "  • Clean Build: $CLEAN_BUILD"
echo "  • Build Admin App: $BUILD_ADMIN"
echo "  • Build Client App: $BUILD_CLIENT"
echo "  • Log File: $LOG_FILE"
echo
echo "Starting build process..."
echo

# Step 1: Environment validation
show_progress "Validating build environment"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Validating build environment" >> "$LOG_FILE"

# Check if Java is available
if ! command -v java &> /dev/null; then
    show_error "Java not found. Please install Java JDK 8 or higher."
    echo "[ERROR] Java not found" >> "$LOG_FILE"
    exit 1
fi

# Check if Gradle wrapper exists
if [ ! -f "$ADMIN_APP_DIR/gradlew" ]; then
    show_error "Gradle wrapper not found in admin-app directory."
    echo "[ERROR] Gradle wrapper not found in admin-app" >> "$LOG_FILE"
    exit 1
fi

if [ ! -f "$CLIENT_APP_DIR/gradlew" ]; then
    show_error "Gradle wrapper not found in android-client-app directory."
    echo "[ERROR] Gradle wrapper not found in android-client-app" >> "$LOG_FILE"
    exit 1
fi

# Make gradlew executable
chmod +x "$ADMIN_APP_DIR/gradlew"
chmod +x "$CLIENT_APP_DIR/gradlew"

# Check keystore files
if [ ! -f "ahk-release-key.jks" ]; then
    show_warning "Admin app keystore (ahk-release-key.jks) not found."
    echo "[WARNING] Admin app keystore not found" >> "$LOG_FILE"
fi

if [ ! -f "ahk-release-key-v2.jks" ]; then
    show_warning "Client app keystore (ahk-release-key-v2.jks) not found."
    echo "[WARNING] Client app keystore not found" >> "$LOG_FILE"
fi

show_success "Environment validation completed"
echo "[SUCCESS] Environment validation completed" >> "$LOG_FILE"

# Step 2: Clean builds (if requested)
if [ "$CLEAN_BUILD" = true ]; then
    if [ "$BUILD_ADMIN" = true ]; then
        show_progress "Cleaning admin app build cache"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cleaning admin app" >> "$LOG_FILE"
        cd "$ADMIN_APP_DIR"
        if ./gradlew clean >> "../$LOG_FILE" 2>&1; then
            show_success "Admin app cleaned successfully"
            echo "[SUCCESS] Admin app cleaned" >> "../$LOG_FILE"
        else
            show_error "Failed to clean admin app"
            echo "[ERROR] Admin app clean failed" >> "../$LOG_FILE"
            ((ERROR_COUNT++))
        fi
        cd ..
    fi
    
    if [ "$BUILD_CLIENT" = true ]; then
        show_progress "Cleaning client app build cache"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cleaning client app" >> "$LOG_FILE"
        cd "$CLIENT_APP_DIR"
        if ./gradlew clean >> "../$LOG_FILE" 2>&1; then
            show_success "Client app cleaned successfully"
            echo "[SUCCESS] Client app cleaned" >> "../$LOG_FILE"
        else
            show_error "Failed to clean client app"
            echo "[ERROR] Client app clean failed" >> "../$LOG_FILE"
            ((ERROR_COUNT++))
        fi
        cd ..
    fi
fi

# Step 3: Build admin app
if [ "$BUILD_ADMIN" = true ]; then
    show_progress "Building admin application ($BUILD_MODE mode)"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Building admin app in $BUILD_MODE mode" >> "$LOG_FILE"
    cd "$ADMIN_APP_DIR"
    
    if [ "$BUILD_MODE" = "debug" ]; then
        BUILD_TASK="assembleDebug"
        ADMIN_APK="app/build/outputs/apk/debug/app-debug.apk"
    else
        BUILD_TASK="assembleRelease"
        ADMIN_APK="app/build/outputs/apk/release/app-release.apk"
    fi
    
    if ./gradlew $BUILD_TASK >> "../$LOG_FILE" 2>&1; then
        show_success "Admin app built successfully"
        echo "[SUCCESS] Admin app built successfully" >> "../$LOG_FILE"
        
        if [ -f "$ADMIN_APK" ]; then
            show_info "Admin APK: $ADMIN_APP_DIR/$ADMIN_APK"
            echo "[INFO] Admin APK location: $ADMIN_APK" >> "../$LOG_FILE"
            
            # Copy and rename APK to root directory
            if [ "$BUILD_MODE" = "debug" ]; then
                copy_apk_to_root "$ADMIN_APK" "debug.apk" "admin"
            else
                copy_apk_to_root "$ADMIN_APK" "admin-release.apk" "admin"
            fi
        fi
    else
        show_error "Admin app build failed"
        echo "[ERROR] Admin app build failed" >> "../$LOG_FILE"
        ((ERROR_COUNT++))
    fi
    cd ..
fi

# Step 4: Build client app
if [ "$BUILD_CLIENT" = true ]; then
    show_progress "Building client application ($BUILD_MODE mode)"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Building client app in $BUILD_MODE mode" >> "$LOG_FILE"
    cd "$CLIENT_APP_DIR"
    
    if [ "$BUILD_MODE" = "debug" ]; then
        BUILD_TASK="assembleDebug"
        CLIENT_APK="app/build/outputs/apk/debug/app-debug.apk"
    else
        BUILD_TASK="assembleRelease"
        CLIENT_APK="app/build/outputs/apk/release/app-release.apk"
    fi
    
    if ./gradlew $BUILD_TASK >> "../$LOG_FILE" 2>&1; then
        show_success "Client app built successfully"
        echo "[SUCCESS] Client app built successfully" >> "../$LOG_FILE"
        
        if [ -f "$CLIENT_APK" ]; then
            show_info "Client APK: $CLIENT_APP_DIR/$CLIENT_APK"
            echo "[INFO] Client APK location: $CLIENT_APK" >> "../$LOG_FILE"
            
            # Copy and rename APK to root directory
            if [ "$BUILD_MODE" = "debug" ]; then
                copy_apk_to_root "$CLIENT_APK" "debug.apk" "client"
            else
                copy_apk_to_root "$CLIENT_APK" "client-release.apk" "client"
            fi
        fi
    else
        show_error "Client app build failed"
        echo "[ERROR] Client app build failed" >> "../$LOG_FILE"
        ((ERROR_COUNT++))
    fi
    cd ..
fi

# Build completion summary
echo
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
if [ $ERROR_COUNT -eq 0 ]; then
    echo -e "${COLOR_SUCCESS}║                            BUILD COMPLETED SUCCESSFULLY                     ║${COLOR_RESET}"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo
    echo "✓ All builds completed without errors"
    echo "✓ Build mode: $BUILD_MODE"
    echo "✓ Log file: $LOG_FILE"
    echo
    if [ "$BUILD_ADMIN" = true ] && [ -n "$ADMIN_APK" ]; then
        echo "✓ Admin APK: $ADMIN_APP_DIR/$ADMIN_APK"
    fi
    if [ "$BUILD_CLIENT" = true ] && [ -n "$CLIENT_APK" ]; then
        echo "✓ Client APK: $CLIENT_APP_DIR/$CLIENT_APK"
    fi
    echo
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Build completed successfully" >> "$LOG_FILE"
    exit 0
else
    echo -e "${COLOR_ERROR}║                              BUILD FAILED                                   ║${COLOR_RESET}"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo
    echo "✗ Build completed with $ERROR_COUNT error(s)"
    echo "✗ Check log file for details: $LOG_FILE"
    echo
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Build failed with $ERROR_COUNT errors" >> "$LOG_FILE"
    exit 1
fi

# Function to copy and rename APK files to root directory
copy_apk_to_root() {
    local SOURCE_APK="$1"
    local TARGET_NAME="$2"
    local APP_TYPE="$3"
    local ROOT_APK="../$TARGET_NAME"
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Copying $APP_TYPE APK to root directory" >> "../$LOG_FILE"
    show_progress "Copying $APP_TYPE APK to root directory as $TARGET_NAME"
    
    if [ -f "$SOURCE_APK" ]; then
        if cp "$SOURCE_APK" "$ROOT_APK" 2>/dev/null; then
            show_success "APK copied successfully: $TARGET_NAME"
            echo "[SUCCESS] APK copied to root: $TARGET_NAME" >> "../$LOG_FILE"
        else
            show_error "Failed to copy APK to root directory"
            echo "[ERROR] Failed to copy APK: $SOURCE_APK to $ROOT_APK" >> "../$LOG_FILE"
            ((ERROR_COUNT++))
        fi
    else
        show_error "Source APK not found: $SOURCE_APK"
        echo "[ERROR] Source APK not found: $SOURCE_APK" >> "../$LOG_FILE"
        ((ERROR_COUNT++))
    fi
}

# ============================================================================
# End of Script
# ============================================================================