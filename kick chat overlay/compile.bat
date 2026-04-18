@echo off
echo Compiling Kick Chat Overlay...

if not exist "bin" mkdir bin

echo.
echo Dependencies found, compiling...
javac -cp "lib/*;src" -d bin src/*.java

if %errorlevel% neq 0 (
    echo.
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo Compilation successful!
echo Compiled classes are in bin/ directory
pause
