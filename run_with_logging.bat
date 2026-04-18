@echo off
echo Running Kick Chat Overlay with full logging...
echo All output will be saved to app_output.log
echo.
echo Starting application...
echo.

REM Run the app and capture ALL output (stdout and stderr)
java -jar KickChatOverlay.jar > app_output.log 2>&1

echo.
echo Application closed.
echo Check app_output.log for all output including errors.
echo.
pause





