@echo off
echo ========================================
echo TikTok Library Test
echo ========================================
echo.
echo This will test if the TikTok library can connect.
echo.
set /p USERNAME="Enter a TikTok username that is LIVE: "
echo.
echo Testing connection to @%USERNAME%...
echo.
echo ========================================
echo.

java -cp "lib/*;bin" TestTikTok %USERNAME%

echo.
echo ========================================
echo Test complete!
echo ========================================
pause
