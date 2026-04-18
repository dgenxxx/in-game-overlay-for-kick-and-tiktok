@echo off
echo ========================================
echo TikTok Connection Diagnostic Tool
echo ========================================
echo.

echo Checking Java version...
java -version
echo.

echo Checking TikTok library files...
if exist "lib\tiktok-client.jar" (
    echo [OK] tiktok-client.jar found
) else (
    echo [ERROR] tiktok-client.jar NOT FOUND
)

if exist "lib\tiktok-api.jar" (
    echo [OK] tiktok-api.jar found
) else (
    echo [ERROR] tiktok-api.jar NOT FOUND
)

if exist "lib\protobuf-java.jar" (
    echo [OK] protobuf-java.jar found
) else (
    echo [ERROR] protobuf-java.jar NOT FOUND
)
echo.

echo Checking compiled classes...
if exist "bin\TikTokClient.class" (
    echo [OK] TikTokClient.class compiled
) else (
    echo [ERROR] TikTokClient.class NOT FOUND - Run compile.bat first
)
echo.

echo ========================================
echo To test TikTok connection:
echo 1. Make sure the TikTok user is LIVE
echo 2. Enter the username (without @) in the app
echo 3. Click Connect
echo 4. Watch the console for error messages
echo ========================================
echo.

echo Press any key to run a quick library test...
pause > nul

echo.
echo Testing TikTok library import...
java -cp "lib/*;bin" TestTikTok testuser 2>&1 | findstr /C:"TikTok library loaded" /C:"ERROR" /C:"Exception"

echo.
echo ========================================
echo Diagnostic complete!
echo ========================================
pause
