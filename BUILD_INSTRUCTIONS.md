# Building the Executable

## Quick Start

1. **Build the executable:**
   - Double-click `build_executable.bat` (recommended - creates self-contained JAR)
   - OR run `build_jar_fat.bat` for a fat JAR (all dependencies included)
   - OR run `build_simple.bat` for a simple JAR (requires lib folder)

2. **Run the application:**
   - Double-click `run.bat`
   - OR run `java -jar KickChatOverlay.jar` from command line

## Build Options

### Option 1: Fat JAR (Recommended)
**File:** `build_jar_fat.bat` or `build_executable.bat`

Creates a self-contained JAR file with all dependencies included. This is the easiest to distribute - users only need the single JAR file.

**Pros:**
- Single file distribution
- No need for lib folder when running
- Easy to share

**Cons:**
- Larger file size (~5-10 MB)

### Option 2: Simple JAR with Classpath
**File:** `build_simple.bat`

Creates a JAR file that references dependencies in the lib folder. The lib folder must be present when running.

**Pros:**
- Smaller JAR file
- Standard Java approach

**Cons:**
- Requires lib folder to be present
- More complex distribution

## Requirements

- Java JDK (for building) or JRE (for running)
- Windows OS
- All dependencies in the `lib` folder

## Distribution

After building, you can distribute:
- **Fat JAR:** Just the `KickChatOverlay.jar` file
- **Simple JAR:** The `KickChatOverlay.jar` file AND the entire `lib` folder

Users can run the JAR by:
- Double-clicking `run.bat`
- Running `java -jar KickChatOverlay.jar` from command line
- Double-clicking the JAR (if Java is properly configured)

