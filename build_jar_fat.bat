@echo off
echo Building Kick Chat Overlay Executable JAR (Fat JAR)...
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

REM Create temporary directory for JAR contents
if exist "temp_jar" rmdir /s /q "temp_jar"
mkdir temp_jar

REM Create JAR from compiled classes first
echo Creating base JAR from compiled classes...
cd bin
jar cf ..\temp_jar\base.jar *
cd ..

REM Extract base JAR to temp directory
echo Extracting base JAR...
cd temp_jar
jar xf base.jar
del base.jar
cd ..

REM Extract all dependency JARs into temp directory
echo Extracting dependencies...
cd lib
for %%f in (*.jar) do (
    echo   Extracting %%f...
    cd ..\temp_jar
    jar xf ..\lib\%%f
    REM Remove META-INF from dependencies to avoid conflicts
    if exist "META-INF\MANIFEST.MF" (
        if not "%%f"=="base.jar" (
            del "META-INF\MANIFEST.MF" 2>nul
        )
    )
    cd ..\lib
)
cd ..

REM Create META-INF directory and manifest file
echo Creating manifest...
if not exist "temp_jar\META-INF" mkdir "temp_jar\META-INF"
(
echo Manifest-Version: 1.0
echo Main-Class: KickChatOverlay
) > temp_jar\META-INF\MANIFEST.MF

REM Create the final fat JAR
echo Creating final fat JAR file...
cd temp_jar
jar cfm ..\KickChatOverlay.jar META-INF\MANIFEST.MF *
cd ..

REM Clean up
echo Cleaning up...
rmdir /s /q "temp_jar"

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
