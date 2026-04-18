@echo off
echo Setting up TikTok Live support...
echo.

if not exist "lib" mkdir lib

echo Downloading TikTokLiveJava Client...
powershell -Command "Invoke-WebRequest -Uri 'https://jitpack.io/com/github/jwdeveloper/TikTok-Live-Java/Client/1.11.0-Release/Client-1.11.0-Release.jar' -OutFile 'lib/tiktok-client.jar'"

echo Downloading TikTokLiveJava API...
powershell -Command "Invoke-WebRequest -Uri 'https://jitpack.io/com/github/jwdeveloper/TikTok-Live-Java/API/1.11.0-Release/API-1.11.0-Release.jar' -OutFile 'lib/tiktok-api.jar'"

echo Downloading TikTokLiveJava Common...
powershell -Command "Invoke-WebRequest -Uri 'https://jitpack.io/com/github/jwdeveloper/TikTok-Live-Java/Common/1.11.0-Release/Common-1.11.0-Release.jar' -OutFile 'lib/tiktok-common.jar'"

echo Downloading Protobuf Java...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.21.12/protobuf-java-3.21.12.jar' -OutFile 'lib/protobuf-java.jar'"

echo.
echo Downloads complete!
echo You can now run compile.bat to build the project with TikTok support.
pause
