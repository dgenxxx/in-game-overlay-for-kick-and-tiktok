import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;
// import io.github.jwdeveloper.tiktok.annotations.TikTokEventHandler; // Not needed for lambdas
import io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokFollowEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokJoinEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokSubscribeEvent;
import io.github.jwdeveloper.tiktok.data.events.room.TikTokRoomInfoEvent;

import java.awt.Color;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.concurrent.*;

import java.time.Duration;

public class TikTokClient {
    private LiveClient client;
    private OverlayWindow overlay;
    private String username;
    private ScheduledExecutorService viewerCountExecutor;
    private ScheduledFuture<?> viewerCountTask;
    
    public TikTokClient(String username, OverlayWindow overlay) {
        this.username = username;
        this.overlay = overlay;
        this.viewerCountExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void connect() {
        // Run in a separate thread to avoid blocking the UI
        new Thread(() -> {
            try {
                System.out.println("========================================");
                System.out.println("TikTokClient: STARTING CONNECTION");
                System.out.println("TikTokClient: Username: @" + username);
                System.out.println("========================================");
                addSystemMessage("Connecting to TikTok: @" + username + "...");
                
                System.out.println("TikTokClient: Step 1 - Building LiveClient...");
                client = TikTokLive.newClient(username)
                    .configure(settings -> {
                        System.out.println("TikTokClient: Step 2 - Configuring settings...");
                        settings.setLogLevel(Level.INFO);
                        settings.setPrintToConsole(true);
                        settings.setClientLanguage("en");
                        System.out.println("TikTokClient: Configuration complete");
                    })
                    .onConnected((liveClient, event) -> {
                        System.out.println("TikTokClient: Successfully connected to @" + username);
                        addSystemMessage("✓ Connected to TikTok: @" + username);
                        
                        // Start polling viewer count every 15 seconds
                        startViewerCountPolling();
                    })
                    .onDisconnected((liveClient, event) -> {
                        System.out.println("TikTokClient: Disconnected from @" + username);
                        addSystemMessage("Disconnected from TikTok");
                    })
                    .onLivePaused((liveClient, event) -> {
                        System.out.println("TikTokClient: Live stream paused");
                        addSystemMessage("⏸ TikTok Live Paused");
                    })
                    .onLiveUnpaused((liveClient, event) -> {
                        System.out.println("TikTokClient: Live stream resumed");
                        addSystemMessage("▶ TikTok Live Resumed");
                    })
                    .onLiveEnded((liveClient, event) -> {
                        System.out.println("TikTokClient: Live stream ended");
                        addSystemMessage("⏹ TikTok Live Ended");
                    })
                    .onRoomInfo((liveClient, event) -> {
                        System.out.println("========================================");
                        System.out.println("TikTokClient: onRoomInfo EVENT FIRED!");
                        try {
                            // Get room info and viewer count
                            io.github.jwdeveloper.tiktok.live.LiveRoomInfo roomInfo = event.getRoomInfo();
                            System.out.println("TikTokClient: RoomInfo object: " + (roomInfo != null ? "NOT NULL" : "NULL"));
                            
                            if (roomInfo != null) {
                                int viewerCount = roomInfo.getViewersCount();
                                int totalViewers = roomInfo.getTotalViewersCount();
                                System.out.println("TikTokClient: Current viewers: " + viewerCount);
                                System.out.println("TikTokClient: Total viewers: " + totalViewers);
                                System.out.println("TikTokClient: Updating overlay with count: " + viewerCount);
                                overlay.updateTikTokViewerCount(viewerCount);
                            }
                        } catch (Exception e) {
                            System.out.println("TikTokClient: Error getting viewer count: " + e.getMessage());
                            e.printStackTrace();
                        }
                        System.out.println("========================================");
                    })
                    .onComment((liveClient, event) -> {
                        try {
                            String userName = event.getUser().getName();
                            System.out.println("TikTokClient: Comment from " + userName + ": " + event.getText());
                            
                            // Show join notification for first-time commenters
                            if (!overlay.hasTikTokViewer(userName)) {
                                System.out.println("TikTokClient: First interaction from " + userName + " - showing join notification");
                                overlay.showJoinNotification(userName);
                            }
                            
                            // Track viewer
                            overlay.addTikTokViewer(userName);
                        } catch (Exception e) {
                            System.out.println("TikTokClient: Comment received");
                        }
                        handleComment(event);
                    })
                    .onGift((liveClient, event) -> {
                        try {
                            String userName = event.getUser().getName();
                            System.out.println("TikTokClient: Gift received from " + userName);
                            // Track viewer
                            overlay.addTikTokViewer(userName);
                        } catch (Exception e) {
                            System.out.println("TikTokClient: Gift received");
                        }
                        handleGift(event);
                    })
                    .onJoin((liveClient, event) -> {
                        System.out.println("========================================");
                        System.out.println("TikTokClient: *** onJoin EVENT FIRED ***");
                        try {
                            String userName = event.getUser().getName();
                            System.out.println("TikTokClient: User joined: " + userName);
                            System.out.println("TikTokClient: Calling showJoinNotification for: " + userName);
                            // Show join notification
                            overlay.showJoinNotification(userName);
                            // Track viewer
                            overlay.addTikTokViewer(userName);
                            System.out.println("TikTokClient: Join notification and tracking complete");
                        } catch (Exception e) {
                            System.out.println("TikTokClient: User joined (error getting name): " + e.getMessage());
                            e.printStackTrace();
                        }
                        System.out.println("========================================");
                    })
                    .onFollow((liveClient, event) -> {
                        try {
                            String userName = event.getUser().getName();
                            System.out.println("TikTokClient: New follower: " + userName);
                            // Track viewer
                            overlay.addTikTokViewer(userName);
                        } catch (Exception e) {
                            System.out.println("TikTokClient: New follower");
                        }
                        handleFollow(event);
                    })
                    .onSubscribe((liveClient, event) -> {
                        try {
                            String userName = event.getUser().getName();
                            System.out.println("TikTokClient: New subscriber: " + userName);
                            // Track viewer
                            overlay.addTikTokViewer(userName);
                        } catch (Exception e) {
                            System.out.println("TikTokClient: New subscriber");
                        }
                        handleSubscribe(event);
                    })
                    .onError((liveClient, event) -> {
                        String errorMsg = event.getException().getMessage();
                        System.err.println("TikTok Error: " + errorMsg);
                        event.getException().printStackTrace();
                        
                        // Show user-friendly error messages
                        if (errorMsg != null && errorMsg.contains("timed out")) {
                            addSystemMessage("⚠ TikTok: Connection timeout - user may be offline");
                        } else if (errorMsg != null && errorMsg.contains("offline")) {
                            addSystemMessage("⚠ TikTok: User is offline");
                        } else {
                            addSystemMessage("⚠ TikTok Error: " + (errorMsg != null ? errorMsg : "Unknown error"));
                        }
                    })
                    .build();
                    
                System.out.println("TikTokClient: Step 3 - Client built successfully");
                System.out.println("TikTokClient: Step 4 - Calling connect()...");
                
                client.connect();
                
                System.out.println("TikTokClient: Step 5 - connect() returned");
                System.out.println("========================================");
                System.out.println("TikTokClient: Connection process completed");
                System.out.println("TikTokClient: Waiting for onConnected callback...");
                System.out.println("========================================");
                    
            } catch (Throwable e) {
                System.err.println("========================================");
                System.err.println("TikTokClient: CONNECTION ERROR");
                System.err.println("Error class: " + e.getClass().getName());
                System.err.println("Error message: " + e.getMessage());
                System.err.println("========================================");
                e.printStackTrace();
                
                // Only show connection errors, not runtime errors (those are handled by onError)
                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = e.getClass().getSimpleName();
                }
                
                // Don't show timeout errors here - they'll be shown by onError handler
                if (!errorMsg.contains("HttpTimeoutException") && !errorMsg.contains("timed out")) {
                    addSystemMessage("✗ Failed to connect to TikTok: " + errorMsg);
                }
            }
        }, "TikTok-Connection-Thread").start();
    }
    
    public void disconnect() {
        if (client != null) {
            try {
                System.out.println("TikTokClient: Disconnecting...");
                
                // Stop viewer count polling
                stopViewerCountPolling();
                
                client.disconnect();
                // Clear all TikTok viewers when disconnecting
                overlay.clearTikTokViewers();
            } catch (Exception e) {
                System.err.println("Error disconnecting TikTok client: " + e.getMessage());
            } finally {
                client = null;
            }
        }
    }
    
    public boolean isConnected() {
        return client != null;
    }
    
    private void handleComment(TikTokCommentEvent event) {
        System.out.println("========================================");
        System.out.println("TikTokClient: *** handleComment CALLED ***");
        try {
            // Get username with fallback
            String user = getUserName(event.getUser());
            String text = event.getText();
            
            System.out.println("TikTokClient: RAW MESSAGE - User: '" + user + "', Text: '" + text + "'");
            System.out.println("TikTokClient: Overlay object: " + (overlay != null ? "NOT NULL" : "NULL"));
            
            if (user == null || user.isEmpty()) {
                System.out.println("TikTokClient: WARNING - Empty username, using default");
                user = "TikTokUser";
            }
            
            if (text == null || text.trim().isEmpty()) {
                System.out.println("TikTokClient: WARNING - Empty message text, skipping");
                System.out.println("========================================");
                return;
            }
            
            // Detect roles
            boolean isMod = false;
            boolean isSub = false;
            boolean isBroadcaster = false;
            
            try {
                isMod = event.getUser().isModerator();
            } catch (Exception e) {
                // Method might not exist in some versions
            }
            
            try {
                isSub = event.getUser().isSubscriber();
            } catch (Exception e) {
                // Method might not exist in some versions
            }
            
            // Check if user is the broadcaster (compare with connected username)
            if (user.equalsIgnoreCase(username)) {
                isBroadcaster = true;
            }
            
            // Generate color using Kick's priority system
            Color userColor = generateColorForUser(user, isBroadcaster, isMod, false, isSub);
            
            ChatMessage msg = new ChatMessage(
                user, 
                text, 
                userColor, 
                new HashMap<>(), // badges
                isSub, 
                isMod, 
                false, // vip
                isBroadcaster,
                "TikTok" // platform
            );
            
            System.out.println("TikTokClient: Created ChatMessage object");
            System.out.println("TikTokClient: Message details - User: " + user + ", Text: " + text + ", Platform: TikTok");
            System.out.println("TikTokClient: Overlay object: " + (overlay != null ? "NOT NULL" : "NULL"));
            System.out.println("TikTokClient: Message object: " + (msg != null ? "NOT NULL" : "NULL"));
            
            if (overlay != null && msg != null) {
                System.out.println("TikTokClient: Calling overlay.addMessage()...");
                overlay.addMessage(msg);
                System.out.println("TikTokClient: overlay.addMessage() completed successfully");
            } else {
                System.err.println("TikTokClient: ERROR - overlay or msg is NULL!");
            }
        } catch (Exception e) {
            System.err.println("TikTokClient: ERROR in handleComment: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("========================================");
    }
    
    private String getUserName(Object user) {
        try {
            // Try getProfileName first
            return (String) user.getClass().getMethod("getProfileName").invoke(user);
        } catch (Exception e) {
            try {
                // Fall back to getName
                return (String) user.getClass().getMethod("getName").invoke(user);
            } catch (Exception e2) {
                return "TikTokUser";
            }
        }
    }
    
    private void handleGift(TikTokGiftEvent event) {
        try {
            String user = getUserName(event.getUser());
            if (user == null || user.isEmpty()) {
                return;
            }
            
            String giftName = event.getGift().getName();
            int count = event.getCombo();
            
            String text = "🎁 sent " + count + "x " + giftName + "!";
            
            boolean isMod = false;
            boolean isSub = false;
            boolean isBroadcaster = user.equalsIgnoreCase(username);
            
            try {
                isMod = event.getUser().isModerator();
                isSub = event.getUser().isSubscriber();
            } catch (Exception e) {
                // Methods might not exist in some versions
            }
            
            // Use role-based color with pink tint for gifts
            Color baseColor = generateColorForUser(user, isBroadcaster, isMod, false, isSub);
            Color giftColor = blendWithPink(baseColor);
            
            ChatMessage msg = new ChatMessage(
                user, 
                text, 
                giftColor,
                new HashMap<>(),
                isSub,
                isMod,
                false,
                isBroadcaster,
                "TikTok"
            );
            
            overlay.addMessage(msg);
        } catch (Exception e) {
            System.err.println("Error handling TikTok gift: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Color blendWithPink(Color baseColor) {
        // Blend base color with pink for gift messages
        int r = (baseColor.getRed() + 255) / 2;
        int g = (baseColor.getGreen() + 105) / 2;
        int b = (baseColor.getBlue() + 180) / 2;
        return new Color(r, g, b);
    }
    
    private void handleFollow(TikTokFollowEvent event) {
        try {
            String user = getUserName(event.getUser());
            if (user == null || user.isEmpty()) {
                return;
            }
            
            ChatMessage msg = new ChatMessage(
                "TikTok", 
                "💚 " + user + " followed!", 
                new Color(0, 255, 127), // Spring green
                new HashMap<>(), 
                false, false, false, false,
                "TikTok"
            );
            overlay.addMessage(msg);
        } catch (Exception e) {
            System.err.println("Error handling TikTok follow: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleSubscribe(TikTokSubscribeEvent event) {
        try {
            String user = getUserName(event.getUser());
            if (user == null || user.isEmpty()) {
                return;
            }
            
            ChatMessage msg = new ChatMessage(
                "TikTok", 
                "⭐ " + user + " subscribed to the LIVE!", 
                new Color(0, 255, 255), // Cyan
                new HashMap<>(), 
                true, false, false, false,
                "TikTok"
            );
            overlay.addMessage(msg);
        } catch (Exception e) {
            System.err.println("Error handling TikTok subscribe: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addSystemMessage(String text) {
        ChatMessage msg = new ChatMessage("System", text, Color.GRAY, new HashMap<>(), false, false, false, false, "System");
        overlay.addMessage(msg);
    }
    
    private Color generateColorForUser(String username, boolean isBroadcaster, boolean isModerator, boolean isVIP, boolean isSubscriber) {
        // Priority-based color assignment matching Kick's scheme
        if (isBroadcaster) {
            return new Color(255, 0, 0); // Red for broadcaster
        } else if (isModerator) {
            return new Color(0, 255, 0); // Green for moderators
        } else if (isVIP) {
            return new Color(255, 215, 0); // Gold for VIPs
        } else if (isSubscriber) {
            return new Color(100, 149, 237); // Cornflower blue for subscribers
        }
        
        // Default: generate color from username hash (same algorithm as Kick)
        java.util.Random random = new java.util.Random();
        int hash = username.hashCode();
        random.setSeed(hash);
        
        int r = 100 + random.nextInt(156);
        int g = 100 + random.nextInt(156);
        int b = 100 + random.nextInt(156);
        
        return new Color(r, g, b);
    }
    
    private void startViewerCountPolling() {
        // Poll viewer count every 5 seconds for more real-time updates
        viewerCountTask = viewerCountExecutor.scheduleAtFixedRate(() -> {
            fetchViewerCount();
        }, 0, 5, TimeUnit.SECONDS);
        System.out.println("TikTokClient: Started viewer count polling (every 5 seconds)");
    }
    
    private void stopViewerCountPolling() {
        if (viewerCountTask != null) {
            viewerCountTask.cancel(false);
            System.out.println("TikTokClient: Stopped viewer count polling");
        }
    }
    
    private void fetchViewerCount() {
        System.out.println("TikTokClient: *** POLLING VIEWER COUNT ***");
        try {
            // Try to get viewer count from the LiveClient
            if (client != null) {
                System.out.println("TikTokClient: Client is NOT NULL");
                try {
                    // Access room info from the client
                    io.github.jwdeveloper.tiktok.live.LiveRoomInfo roomInfo = client.getRoomInfo();
                    System.out.println("TikTokClient: RoomInfo from client: " + (roomInfo != null ? "NOT NULL" : "NULL"));
                    
                    if (roomInfo != null) {
                        int viewerCount = roomInfo.getViewersCount();
                        int totalViewers = roomInfo.getTotalViewersCount();
                        System.out.println("TikTokClient: Polled current viewers: " + viewerCount);
                        System.out.println("TikTokClient: Polled total viewers: " + totalViewers);
                        System.out.println("TikTokClient: Updating overlay with polled count: " + viewerCount);
                        overlay.updateTikTokViewerCount(viewerCount);
                        return;
                    } else {
                        System.out.println("TikTokClient: RoomInfo is NULL - cannot get viewer count");
                    }
                } catch (Exception e) {
                    System.out.println("TikTokClient: Exception accessing room info: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("TikTokClient: Client is NULL - not connected");
            }
            
            System.out.println("TikTokClient: Could not fetch viewer count");
        } catch (Exception e) {
            System.err.println("TikTokClient: Error fetching viewer count: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
