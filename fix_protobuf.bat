@echo off
echo Fixing Protobuf Dependency for TikTok...
echo.
echo This will download the correct version of protobuf-java (4.26.1)
echo which is required for TikTok Live integration (includes RuntimeVersion$RuntimeDomain).
echo.

if not exist "lib" mkdir lib

echo Downloading Protobuf Java 4.26.1...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/4.26.1/protobuf-java-4.26.1.jar' -OutFile 'lib/protobuf-java.jar'"

if %errorlevel% neq 0 (
    echo.
    echo Download failed! Trying alternative method...
    echo.
    echo Please manually download protobuf-java-3.21.12.jar from:
    echo https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.21.12/
    echo.
    echo Save it as: lib\protobuf-java.jar
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Protobuf updated successfully!
echo ========================================
echo.
echo You may need to rebuild the JAR file:
echo   1. Run build_executable.bat to rebuild
echo   2. Or just run the app - it should work now
echo.
pause

