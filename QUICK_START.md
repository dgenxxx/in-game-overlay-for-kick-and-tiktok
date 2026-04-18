# Quick Start Guide - How to Run Kick Chat Overlay

## Prerequisites

1. **Java Runtime Environment (JRE) or Java Development Kit (JDK)**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Or use OpenJDK: https://adoptium.net/
   - Make sure Java is in your system PATH

2. **Verify Java Installation**
   - Open Command Prompt or PowerShell
   - Type: `java -version`
   - You should see something like: `java version "17.0.x"` or similar

## Running the Application

### Option 1: Using the Run Script (Easiest)

1. **Double-click `run.bat`**
   - This will automatically start the application
   - If the JAR doesn't exist, it will tell you to build it first

### Option 2: Using Command Line

1. **Open Command Prompt or PowerShell** in the application folder

2. **Run the JAR file:**
   ```bash
   java -jar KickChatOverlay.jar
   ```

   Or if that doesn't work:
   ```bash
   java -cp "lib/*;KickChatOverlay.jar" KickChatOverlay
   ```

### Option 3: Build First (If JAR doesn't exist)

If you don't have `KickChatOverlay.jar` yet:

1. **Double-click `build_executable.bat`**
   - This will compile the code and create the executable JAR
   - Wait for the build to complete

2. **Then run using Option 1 or 2 above**

## Using the Application

1. **Control Panel opens automatically**
   - This is the main window where you configure settings

2. **Connect to Kick:**
   - Enter your Kick channel name or ID in the "Kick Channel/ID" field
   - Click "Connect"

3. **Connect to TikTok (Optional):**
   - Enter your TikTok username in the "TikTok User" field
   - Click "Connect" (works with Kick connection)

4. **Adjust Settings:**
   - **Transparency**: Control how transparent the overlay background is
   - **Msg Fade Time**: How long messages stay visible (in seconds)
   - **Sound Effects**: Enable/disable notification sounds
   - **Lock Overlay**: Make overlay click-through (visible but doesn't block mouse)

5. **Test the Overlay:**
   - Click "Test Overlay" to see a sample message
   - The overlay window appears separately from the control panel

6. **Customize Colors:**
   - Click "Colors" to set custom colors for different user roles

## Troubleshooting

### "Java is not recognized"
- Java is not installed or not in PATH
- Install Java and make sure it's added to system PATH
- Restart your computer after installing

### "KickChatOverlay.jar not found"
- Run `build_executable.bat` first to create the JAR file

### "Error connecting to Kick"
- Make sure you entered the correct channel name/ID
- Check your internet connection
- Some channels may require authentication (enter auth token if needed)

### "TikTok library not found"
- Make sure all JAR files are in the `lib` folder
- Required files: `tiktok-client.jar`, `tiktok-api.jar`, `protobuf-java.jar`

### "NoClassDefFoundError: com/google/protobuf/MapFieldReflectionAccessor"
- **This is a protobuf version issue**
- Run `fix_tiktok_dependencies.bat` to download the correct protobuf version (3.21.12)
- Or manually download from: https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.21.12/
- After updating, rebuild the JAR: `build_executable.bat`

### Application won't start
- Check that all files are in the correct folders
- Make sure the `lib` folder exists with all dependencies
- Try running from command line to see error messages

## File Structure

```
kick chat overlay/
├── KickChatOverlay.jar    ← Executable file (run this)
├── run.bat                ← Easy launcher (double-click this)
├── build_executable.bat   ← Build script (if needed)
├── lib/                   ← Required libraries
│   ├── tiktok-client.jar
│   ├── tiktok-api.jar
│   ├── gson-2.10.1.jar
│   └── ... (other dependencies)
├── src/                   ← Source code
└── config.properties      ← Settings (auto-created)
```

## Need Help?

- Check the console output for error messages
- Make sure Java version is 11 or higher
- Verify all files are in the correct locations

