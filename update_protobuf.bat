@echo off
echo Updating protobuf to version 4.26.1 (includes RuntimeVersion$RuntimeDomain)...
echo.

REM Delete old version
if exist "lib\protobuf-java.jar" del "lib\protobuf-java.jar"

REM Download correct version
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/4.26.1/protobuf-java-4.26.1.jar' -OutFile 'lib\protobuf-java.jar'"

if %errorlevel% neq 0 (
    echo Download failed!
    pause
    exit /b 1
)

echo.
echo Protobuf updated! Now rebuild the JAR:
echo   build_jar_fat.bat
echo.
pause

