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

REM Create temporary manifest file with Main-Class
echo Creating manifest...
(
echo Manifest-Version: 1.0
echo Main-Class: KickChatOverlay
echo Class-Path: .
) > manifest.txt

REM Create JAR file with compiled classes
echo Creating JAR file...
jar cfm KickChatOverlay.jar manifest.txt -C bin .

REM Extract all dependency JARs and add to main JAR
echo Adding dependencies to JAR...
cd lib
for %%f in (*.jar) do (
    echo   Adding %%f...
    jar uf ..\KickChatOverlay.jar -C . %%f
    cd ..
    jar xf lib\%%f
    cd lib
)
cd ..

REM Clean up extracted files
echo Cleaning up...
if exist "META-INF" rmdir /s /q "META-INF"
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
pause

