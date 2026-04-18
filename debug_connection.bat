@echo off
cls
echo ╔════════════════════════════════════════════════════════════════╗
echo ║         KICK + TIKTOK CONNECTION DEBUG TOOL                    ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

:menu
echo [1] Test TikTok Library Only (Minimal Test)
echo [2] Run Full App with Debug Logging
echo [3] Check Library Files
echo [4] Test Both Platforms Simultaneously
echo [5] Exit
echo.
set /p choice="Select option (1-5): "

if "%choice%"=="1" goto tiktok_test
if "%choice%"=="2" goto full_app
if "%choice%"=="3" goto check_libs
if "%choice%"=="4" goto both_test
if "%choice%"=="5" goto end
goto menu

:tiktok_test
cls
echo ========================================
echo TikTok Library Test
echo ========================================
echo.
echo IMPORTANT: The TikTok user MUST be LIVE right now!
echo.
set /p ttuser="Enter TikTok username (without @): "
echo.
echo Testing connection to @%ttuser%...
echo Watch for [SUCCESS] Connected! message
echo.
java -cp "lib/*;bin" MinimalTikTokTest %ttuser%
echo.
pause
goto menu

:full_app
cls
echo ========================================
echo Running Full App with Debug Logging
echo ========================================
echo.
echo This will show ALL console output.
echo Keep this window open to see real-time logs!
echo.
echo Instructions:
echo 1. Enter Kick channel ID and/or TikTok username
echo 2. Click Connect
echo 3. Watch THIS console window for messages
echo.
pause
echo.
java -jar KickChatOverlay.jar
pause
goto menu

:check_libs
cls
echo ========================================
echo Checking Library Files
echo ========================================
echo.

if exist "lib\tiktok-client.jar" (
    echo [OK] tiktok-client.jar found
) else (
    echo [MISSING] tiktok-client.jar NOT FOUND!
)

if exist "lib\tiktok-api.jar" (
    echo [OK] tiktok-api.jar found
) else (
    echo [MISSING] tiktok-api.jar NOT FOUND!
)

if exist "lib\protobuf-java.jar" (
    echo [OK] protobuf-java.jar found
) else (
    echo [MISSING] protobuf-java.jar NOT FOUND!
)

if exist "lib\gson-2.10.1.jar" (
    echo [OK] gson-2.10.1.jar found
) else (
    echo [MISSING] gson-2.10.1.jar NOT FOUND!
)

if exist "lib\Java-WebSocket-1.5.3.jar" (
    echo [OK] Java-WebSocket-1.5.3.jar found
) else (
    echo [MISSING] Java-WebSocket-1.5.3.jar NOT FOUND!
)

echo.
echo Checking compiled classes...
if exist "bin\TikTokClient.class" (
    echo [OK] TikTokClient.class compiled
) else (
    echo [MISSING] TikTokClient.class - Run compile.bat!
)

if exist "bin\KickWebSocketClient.class" (
    echo [OK] KickWebSocketClient.class compiled
) else (
    echo [MISSING] KickWebSocketClient.class - Run compile.bat!
)

echo.
pause
goto menu

:both_test
cls
echo ========================================
echo Testing Both Platforms Simultaneously
echo ========================================
echo.
echo This will test if Kick and TikTok can work together.
echo.
set /p kickchan="Enter Kick channel ID (or leave empty): "
set /p ttuser="Enter TikTok username (or leave empty): "
echo.

if "%kickchan%"=="" if "%ttuser%"=="" (
    echo ERROR: You must enter at least one platform!
    pause
    goto menu
)

echo Starting app...
echo.
echo INSTRUCTIONS:
echo 1. The app window will open
echo 2. Fields should be pre-filled
echo 3. Click Connect
echo 4. Watch THIS console for debug messages
echo.
echo Look for these messages:
if not "%kickchan%"=="" echo   - "WebSocket Connected to Kick"
if not "%ttuser%"=="" echo   - "TikTokClient: Successfully connected"
echo.
pause
echo.
java -jar KickChatOverlay.jar
pause
goto menu

:end
echo.
echo Exiting...
exit /b
