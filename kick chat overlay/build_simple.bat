@echo off
echo Building Kick Chat Overlay Executable JAR...
echo.

REM Create bin directory if it doesn't exist
if not exist "bin" mkdir bin

REM Compile Java source files
echo Compiling Java source files...
javac -cp "lib/*;src" -d bin src/*.java

if %errorlevel% neq 0 (
    echo.
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo Compilation successful!
echo.

REM Build classpath string for manifest
echo Building classpath...
set "classpath=."
cd lib
for %%f in (*.jar) do (
    set "classpath=!classpath! lib\%%f"
)
cd ..
setlocal enabledelayedexpansion

REM Create manifest file with Main-Class and Class-Path
echo Creating manifest...
(
echo Manifest-Version: 1.0
echo Main-Class: KickChatOverlay
echo Class-Path: !classpath!
) > manifest.txt
endlocal

REM Create JAR file
echo Creating JAR file...
jar cfm KickChatOverlay.jar manifest.txt -C bin .

REM Clean up
del manifest.txt

echo.
echo ========================================
echo Build complete! 
echo Executable: KickChatOverlay.jar
echo ========================================
echo.
echo You can now run the application using:
echo   java -jar KickChatOverlay.jar
echo.
echo Or double-click run.bat
echo.
echo Note: Make sure the lib folder is in the same directory as the JAR.
echo.
pause

