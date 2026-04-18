# TikTok Chat - Quick Start Guide

## ✅ TikTok Chat is Now Live!

Your Kick Chat Overlay now supports TikTok LIVE chat messages!

## How to Connect

1. **Launch the application**
   - Run `run.bat` or double-click `KickChatOverlay.jar`

2. **Enter TikTok username**
   - In the control panel, find the "TikTok User:" field
   - Enter the TikTok username (without @)
   - Example: If the profile is @username, just enter: username

3. **Click Connect**
   - The overlay will show "Connecting to TikTok: @username..."
   - Wait for "✓ Connected to TikTok: @username"
   - You'll see "Listening for chat messages..."

4. **Start seeing messages!**
   - All chat comments will appear in the overlay
   - Gifts, follows, and subscriptions also show up

## What You'll See

### Chat Messages
```
Username: Hello from TikTok!
```
- Color-coded by role (moderators = green, subscribers = pink)

### Gifts
```
Username: 🎁 sent 5x Rose!
```
- Hot pink color for visibility

### Follows
```
TikTok: 💚 Username followed!
```
- Spring green color

### Subscriptions
```
TikTok: ⭐ Username subscribed to the LIVE!
```
- Cyan color

## Dual Platform Support

You can connect to **both Kick and TikTok** at the same time:
1. Enter your Kick channel ID
2. Enter your TikTok username
3. Click Connect
4. Both chats will appear in the same overlay!

## Troubleshooting

### "TikTok library not installed" error
- Make sure `tiktok-client.jar` and `tiktok-api.jar` are in the `lib/` folder
- Also need `protobuf-java.jar`
- Run `setup_tiktok.bat` if you haven't already

### No messages appearing
- Make sure the TikTok user is currently LIVE
- Check the console for connection messages
- Verify the username is correct (no @ symbol)

### Connection fails
- The TikTok user must be streaming LIVE
- Check your internet connection
- Look for error messages in the overlay

## Features Work with TikTok

All overlay features work with TikTok messages:
- ✅ Transparency slider
- ✅ Message fade timer
- ✅ Sound effects
- ✅ Click-through lock mode
- ✅ Drag and resize
- ✅ Color customization

## Need Help?

Check the console output for detailed logs. All TikTok events are logged with "TikTokClient:" prefix.
