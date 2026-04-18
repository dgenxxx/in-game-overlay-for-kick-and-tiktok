@echo off
echo Starting Kick Chat Overlay...
echo.

REM Check if JAR exists
if not exist "KickChatOverlay.jar" (
    echo Error: KickChatOverlay.jar not found!
    echo Please run build_executable.bat or build_jar_fat.bat first to create the executable.
    echo.
    pause
    exit /b 1
)

REM Check if lib folder exists
if not exist "lib" (
    echo Error: lib folder not found!
    echo Make sure you're running from the correct directory.
    echo.
    pause
    exit /b 1
)

REM Try to run with -jar first (works if classpath is set in manifest)
java -jar KickChatOverlay.jar 2>nul

REM If that fails, run with explicit classpath
if %errorlevel% neq 0 (
    echo Running with explicit classpath...
    setlocal enabledelayedexpansion
    set "cp=lib\*"
    java -cp "!cp!;KickChatOverlay.jar" KickChatOverlay
    if !errorlevel! neq 0 (
        echo.
        echo Application failed to start.
        echo Make sure Java is installed and in your PATH.
        echo.
        pause
    )
    endlocal
)

