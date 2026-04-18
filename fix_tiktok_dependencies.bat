@echo off
echo ========================================
echo Fixing TikTok Dependencies
echo ========================================
echo.
echo This will download/update all required TikTok dependencies:
echo   - protobuf-java 3.21.12 (fixes MapFieldReflectionAccessor error)
echo   - TikTok Client libraries (if missing)
echo.

if not exist "lib" mkdir lib

echo.
echo Step 1: Downloading Protobuf Java 4.26.1 (required for RuntimeVersion$RuntimeDomain)...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/4.26.1/protobuf-java-4.26.1.jar' -OutFile 'lib/protobuf-java.jar'"

if %errorlevel% neq 0 (
    echo ERROR: Failed to download protobuf-java
    echo Please check your internet connection and try again.
    pause
    exit /b 1
)

echo ✓ Protobuf downloaded successfully
echo.

REM Check if TikTok libraries exist
if not exist "lib\tiktok-client.jar" (
    echo Step 2: Downloading TikTok Client...
    powershell -Command "Invoke-WebRequest -Uri 'https://jitpack.io/com/github/jwdeveloper/TikTok-Live-Java/Client/1.11.0-Release/Client-1.11.0-Release.jar' -OutFile 'lib/tiktok-client.jar'"
    if %errorlevel% equ 0 (
        echo ✓ TikTok Client downloaded
    ) else (
        echo ⚠ TikTok Client download failed (may already exist)
    )
    echo.
)

if not exist "lib\tiktok-api.jar" (
    echo Step 3: Downloading TikTok API...
    powershell -Command "Invoke-WebRequest -Uri 'https://jitpack.io/com/github/jwdeveloper/TikTok-Live-Java/API/1.11.0-Release/API-1.11.0-Release.jar' -OutFile 'lib/tiktok-api.jar'"
    if %errorlevel% equ 0 (
        echo ✓ TikTok API downloaded
    ) else (
        echo ⚠ TikTok API download failed (may already exist)
    )
    echo.
)

if not exist "lib\tiktok-common.jar" (
    echo Step 4: Downloading TikTok Common (REQUIRED)...
    powershell -Command "Invoke-WebRequest -Uri 'https://jitpack.io/com/github/jwdeveloper/TikTok-Live-Java/Common/1.11.0-Release/Common-1.11.0-Release.jar' -OutFile 'lib/tiktok-common.jar'"
    if %errorlevel% equ 0 (
        echo ✓ TikTok Common downloaded
    ) else (
        echo ⚠ TikTok Common download failed (may already exist)
    )
    echo.
)

echo.
echo ========================================
echo Dependencies updated!
echo ========================================
echo.
echo Next steps:
echo   1. If you have a JAR file, rebuild it: build_executable.bat
echo   2. Or just run the app - it should work now: run.bat
echo.
pause

