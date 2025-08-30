# AHK Finance Build Script Guide

## Overview

The AHK Finance Build Script is a professional one-click build system designed to automate the entire build process for both the Admin and Client Android applications. It provides a clean, intuitive interface with progress indicators, comprehensive logging, and cross-platform compatibility.

## Features

- ✅ **Professional UI** with progress indicators and color-coded output
- ✅ **Cross-platform compatibility** (Windows batch and Unix shell scripts)
- ✅ **Comprehensive logging** with timestamped output
- ✅ **Environment validation** for Java, Android SDK, and keystores
- ✅ **Flexible build modes** (debug/release)
- ✅ **Selective building** (admin-only, client-only, or both)
- ✅ **Clean build support** with optional cache clearing
- ✅ **Error handling** with detailed feedback
- ✅ **Build artifact tracking** with APK location display

## Quick Start

### Windows
```batch
# Build both apps in release mode (default)
.\build.bat

# Build both apps in debug mode
.\build.bat --debug

# Build only admin app
.\build.bat --admin-only

# Build only client app
.\build.bat --client-only

# Skip clean step for faster builds
.\build.bat --no-clean
```

### Unix/Linux/macOS
```bash
# Build both apps in release mode (default)
./build.sh

# Build both apps in debug mode
./build.sh --debug

# Build only admin app
./build.sh --admin-only

# Build only client app
./build.sh --client-only

# Skip clean step for faster builds
./build.sh --no-clean
```

## Command Line Options

| Option | Description |
|--------|-------------|
| `--help`, `-h` | Show help message and usage examples |
| `--debug` | Build in debug mode (default: release) |
| `--release` | Build in release mode (explicit) |
| `--no-clean` | Skip clean build step for faster builds |
| `--admin-only` | Build only the admin application |
| `--client-only` | Build only the client application |
| `--verbose` | Enable verbose logging (future enhancement) |

## Build Modes

### Release Mode (Default)
- Optimized for production deployment
- Uses release signing configuration
- Generates signed APKs ready for distribution
- Output: `app-release.apk`

### Debug Mode
- Optimized for development and testing
- Uses debug signing configuration
- Faster build times
- Output: `app-debug.apk`

## Project Structure

```
ahkemi/
├── build.bat                    # Windows build script
├── build.sh                     # Unix/Linux/macOS build script
├── BUILD_GUIDE.md              # This documentation
├── admin-app/                  # Admin application source
│   ├── gradlew.bat
│   ├── gradlew
│   └── app/build.gradle
├── android-client-app/         # Client application source
│   ├── gradlew.bat
│   ├── gradlew
│   └── app/build.gradle
├── ahk-release-key.jks         # Admin app keystore
├── ahk-release-key-v2.jks      # Client app keystore
└── build-logs/                 # Generated build logs
    └── build-YYYYMMDD-HHMMSS.log
```

## Prerequisites

### Required Software
1. **Java Development Kit (JDK)** 8 or higher
2. **Android SDK** with build tools
3. **Gradle** (included via wrapper)

### Required Files
- `admin-app/gradlew.bat` (Windows) or `admin-app/gradlew` (Unix)
- `android-client-app/gradlew.bat` (Windows) or `android-client-app/gradlew` (Unix)
- `ahk-release-key.jks` (Admin app keystore)
- `ahk-release-key-v2.jks` (Client app keystore)

### Environment Validation

The build script automatically validates:
- Java installation and version
- Gradle wrapper availability
- Keystore file presence
- Directory structure integrity

## Build Process

The build script follows this systematic process:

1. **Parse Arguments** - Process command line options
2. **Environment Validation** - Check prerequisites
3. **Clean Builds** (optional) - Clear build cache
4. **Build Admin App** (if selected)
5. **Build Client App** (if selected)
6. **Generate Summary** - Display results and APK locations

## Output Locations

### Admin App APKs
- **Debug**: `admin-app/app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `admin-app/app/build/outputs/apk/release/app-release.apk`

### Client App APKs
- **Debug**: `android-client-app/app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `android-client-app/app/build/outputs/apk/release/app-release.apk`

## Logging

### Log Files
Build logs are automatically generated in the `build-logs/` directory with the format:
```
build-YYYYMMDD-HHMMSS.log
```

### Log Content
- Build start time and command line arguments
- Environment validation results
- Step-by-step build progress
- Error messages and stack traces
- Build completion status and timing
- APK output locations

## Error Handling

### Common Issues and Solutions

#### Java Not Found
```
[ERROR] Java not found. Please install Java JDK 8 or higher.
```
**Solution**: Install Java JDK and ensure it's in your system PATH.

#### Gradle Wrapper Missing
```
[ERROR] Gradle wrapper not found in admin-app directory.
```
**Solution**: Ensure the project structure is intact and gradlew files exist.

#### Keystore Missing
```
[WARNING] Admin app keystore (ahk-release-key.jks) not found.
```
**Solution**: Place the required keystore files in the project root directory.

#### Build Failed
```
[ERROR] Admin app build failed
```
**Solution**: Check the detailed log file for specific error messages and resolve dependencies or code issues.

## Performance Tips

### Faster Builds
1. Use `--no-clean` to skip cache clearing
2. Build individual apps with `--admin-only` or `--client-only`
3. Use debug mode for development iterations
4. Ensure adequate system resources (RAM, disk space)

### Parallel Builds
The script builds apps sequentially to ensure stability. For parallel builds, run separate instances:
```batch
# Terminal 1
.\build.bat --admin-only

# Terminal 2
.\build.bat --client-only
```

## Customization

### Modifying Build Configuration
Edit the configuration variables at the top of the build script:

```batch
:: Configuration Variables
set "SCRIPT_VERSION=2.0"
set "PROJECT_NAME=AHK Finance"
set "ADMIN_APP_DIR=admin-app"
set "CLIENT_APP_DIR=android-client-app"
```

### Adding New Build Variants
To add new build variants, modify the Gradle commands in the build sections:

```batch
:: Example: Add staging build
if "%BUILD_MODE%"=="staging" (
    call gradlew.bat assembleStaging >> ".\%LOG_FILE%" 2>&1
)
```

## Troubleshooting

### Debug Mode
For detailed troubleshooting, examine the log files in `build-logs/` directory. The logs contain:
- Complete Gradle output
- Error stack traces
- Environment validation details
- Timing information

### Manual Build
If the script fails, you can manually build each app:

```batch
# Admin app
cd admin-app
gradlew.bat assembleRelease
cd ..

# Client app
cd android-client-app
gradlew.bat assembleRelease
cd ..
```

### Clean Reset
To completely reset the build environment:

```batch
# Clean all build artifacts
cd admin-app
gradlew.bat clean
cd ..
cd android-client-app
gradlew.bat clean
cd ..

# Remove build logs
rmdir /s /q build-logs
```

## Support

For issues or questions:
1. Check the build logs for detailed error information
2. Verify all prerequisites are installed
3. Ensure project structure is intact
4. Review this documentation for common solutions

## Version History

### v2.0 (Current)
- Professional UI with progress indicators
- Cross-platform compatibility
- Comprehensive logging system
- Environment validation
- Flexible build options
- Error handling and recovery

---

**AHK Finance Build Script v2.0** - Professional Android Build System