# TikTok Connection Troubleshooting Guide

## Updated Implementation

The TikTok client has been updated with:
- ✅ Better error handling and logging
- ✅ API compatibility fixes (getName() fallback)
- ✅ Detailed console output for debugging
- ✅ Specific error messages for common issues

## How to Test

1. **Run the diagnostic tool first:**
   ```
   diagnose_tiktok.bat
   ```
   This will check if all libraries are present.

2. **Launch the app with logging:**
   ```
   run_with_logging.bat
   ```
   Or just:
   ```
   run.bat
   ```

3. **Watch the console output** - You'll see detailed messages like:
   ```
   ========================================
   TikTokClient: Starting connection process
   TikTokClient: Target username: @username
   ========================================
   ```

## Common Issues & Solutions

### Issue 1: "TikTok library not installed"
**Cause:** Missing JAR files in lib folder

**Solution:**
1. Check that these files exist in `lib/` folder:
   - `tiktok-client.jar`
   - `tiktok-api.jar`
   - `protobuf-java.jar`
2. If missing, run `setup_tiktok.bat` or download them manually

### Issue 2: "User is not currently LIVE"
**Cause:** The TikTok user you're trying to connect to is offline

**Solution:**
- Make sure the TikTok user is actively streaming LIVE
- TikTok LIVE Java library can only connect to active streams
- Try a different user who is currently LIVE

### Issue 3: "TikTok user not found"
**Cause:** Username is incorrect or doesn't exist

**Solution:**
- Double-check the username spelling
- Enter username WITHOUT the @ symbol
- Example: For @username, just enter: username

### Issue 4: Connection hangs or times out
**Cause:** Network issues or TikTok API blocking

**Solution:**
- Check your internet connection
- Try a different network (TikTok may block some IPs)
- Wait a few minutes and try again
- Some regions may have restricted access

### Issue 5: No messages appearing
**Cause:** Connected but events not firing

**Solution:**
- Check console for "Comment from" messages
- Make sure people are actually chatting
- Try sending a test message yourself
- Verify the stream has chat enabled

## Debugging Steps

1. **Enable full logging:**
   - The app now uses `Level.ALL` for maximum debug output
   - Watch the console for detailed connection steps

2. **Check for specific errors:**
   ```
   TikTokClient: CONNECTION FAILED
   Error type: [error class name]
   Error message: [detailed message]
   ```

3. **Verify library loading:**
   - Look for "TikTokClient: Creating LiveClient instance..."
   - If you see "NoClassDefFoundError", libraries are missing

4. **Test with a known LIVE user:**
   - Find a popular TikTok streamer who is currently LIVE
   - Try connecting to them first to verify the system works

## What the Console Should Show (Success)

```
========================================
TikTokClient: Starting connection process
TikTokClient: Target username: @username
========================================
TikTokClient: Creating LiveClient instance...
TikTokClient: Configuring client settings...
TikTokClient: Successfully connected to @username
TikTokClient: buildAndConnect() completed
TikTokClient: Client state: Created
========================================
```

Then when messages come in:
```
TikTokClient: Comment from User123: Hello!
TikTokClient: Adding message from User123: Hello!
```

## What the Console Shows (Failure)

```
========================================
TikTokClient: CONNECTION FAILED
Error type: SomeException
Error message: [reason]
========================================
[Stack trace]
```

## Still Not Working?

1. **Check Java version:**
   ```
   java -version
   ```
   Should be Java 8 or higher

2. **Rebuild everything:**
   ```
   compile.bat
   build_jar.bat
   ```

3. **Try the test program:**
   ```
   java -cp "lib/*;bin" TestTikTok username
   ```
   Replace `username` with an actual TikTok username that's LIVE

4. **Check the overlay window:**
   - Make sure it's visible
   - Check if system messages appear
   - Try the "Test Overlay" button first

## Known Limitations

- ✅ TikTok user MUST be LIVE (not offline)
- ✅ Some regions may have restricted access
- ✅ TikTok may rate-limit connections
- ✅ Private/restricted streams may not work
- ✅ Very new accounts might have issues

## Getting Help

When reporting issues, include:
1. Full console output (copy everything)
2. TikTok username you're trying to connect to
3. Whether that user is currently LIVE
4. Your Java version
5. Any error messages from the overlay
