@echo off
cls
echo ╔════════════════════════════════════════════════════════════════╗
echo ║          TIKTOK CONNECTION DIAGNOSTIC TOOL                     ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo This tool will help identify why TikTok is not connecting.
echo.
echo ========================================
echo STEP 1: Enter TikTok Username
echo ========================================
echo.
set /p USERNAME="Enter TikTok username (without @): "

if "%USERNAME%"=="" (
    echo ERROR: Username cannot be empty!
    pause
    exit /b
)

echo.
echo ========================================
echo STEP 2: Testing Connection
echo ========================================
echo.
echo Testing: @%USERNAME%
echo.
echo This will:
echo 1. Check if user is LIVE
echo 2. Attempt to connect
echo 3. Wait for chat messages
echo.
echo Watch for these messages:
echo   [1] Checking if user is LIVE...
echo   [2] Creating TikTok client...
echo   [3] Client created: YES
echo   [SUCCESS] CONNECTED SUCCESSFULLY!
echo.
pause
echo.
echo ========================================
echo STARTING TEST...
echo ========================================
echo.

java -cp "lib/*;bin" SimpleTikTokTest %USERNAME%

echo.
echo ========================================
echo TEST COMPLETE
echo ========================================
echo.
echo What did you see?
echo.
echo [A] "USER IS OFFLINE" - The user is not currently LIVE
echo     Solution: Try a different user who is streaming right now
echo.
echo [B] "CONNECTED SUCCESSFULLY" - Connection worked!
echo     If you saw chat messages, TikTok is working correctly.
echo     If no messages appeared, the stream might have no chat activity.
echo.
echo [C] Error messages - There's a problem with the library or network
echo     Copy the error message and check TIKTOK_TROUBLESHOOTING.md
echo.
echo [D] Nothing happened - The library might not be loaded correctly
echo     Run: debug_connection.bat → Option 3 to check libraries
echo.
pause
