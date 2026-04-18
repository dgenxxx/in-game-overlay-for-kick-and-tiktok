import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;
// import io.github.jwdeveloper.tiktok.annotations.TikTokEventHandler; // Not needed for lambdas
import io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokFollowEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokSubscribeEvent;

import java.awt.Color;
import java.util.HashMap;
import java.util.logging.Level;

import java.time.Duration;

public class TikTokClient {
    private LiveClient client;
    private OverlayWindow overlay;
    private String username;
    
    public TikTokClient(String username, OverlayWindow overlay) {
        this.username = username;
        this.overlay = overlay;
    }
    
    public void connect() {
        // Run in a separate thread to avoid blocking the UI
        new Thread(() -> {
            try {
                System.out.println("TikTokClient: Connecting to " + username);
                addSystemMessage("Connecting to TikTok: " + username + "...");
                
                client = TikTokLive.newClient(username)
                    .configure(settings -> {
                        settings.setLogLevel(Level.ALL); // Enable ALL logs to see what's happening
                        settings.setPrintToConsole(true);
                        // settings.setTimeout(Duration.ofSeconds(20)); // Method not available in LiveClientSettings
                    })
                    .onConnected((liveClient, event) -> {
                        System.out.println("TikTokClient: Connected to " + username);
                        addSystemMessage("Connected to TikTok: " + username);
                    })
                    .onDisconnected((liveClient, event) -> {
                        System.out.println("TikTokClient: Disconnected");
                        addSystemMessage("Disconnected from TikTok");
                    })
                    .onLivePaused((liveClient, event) -> {
                        System.out.println("TikTokClient: Live Paused");
                        addSystemMessage("TikTok Live Paused");
                    })
                    .onLiveUnpaused((liveClient, event) -> {
                        System.out.println("TikTokClient: Live Unpaused");
                        addSystemMessage("TikTok Live Resumed");
                    })
                    .onLiveEnded((liveClient, event) -> {
                         System.out.println("TikTokClient: Live Ended");
                         addSystemMessage("TikTok Live Ended");
                    })
                    .onComment((liveClient, event) -> {
                        System.out.println("TikTokClient: Comment received: " + event.getText());
                        handleComment(event);
                    })
                    .onGift((liveClient, event) -> {
                        handleGift(event);
                    })
                    .onFollow((liveClient, event) -> {
                        handleFollow(event);
                    })
                    .onSubscribe((liveClient, event) -> {
                        handleSubscribe(event);
                    })
                    .onError((liveClient, event) -> {
                        System.err.println("TikTok Error: " + event.getException().getMessage());
                        event.getException().printStackTrace();
                        addSystemMessage("TikTok Error: " + event.getException().getMessage());
                    })
                    .buildAndConnect();
                    
            } catch (Exception e) {
                e.printStackTrace();
                addSystemMessage("Failed to connect to TikTok: " + e.getMessage());
            }
        }).start();
    }
    
    public void disconnect() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }
    
    private void handleComment(TikTokCommentEvent event) {
        String user = event.getUser().getProfileName();
        String text = event.getText();
        // Basic mapping: TikTok doesn't have exact Kick roles, but we can map what we have
        boolean isMod = event.getUser().isModerator();
        boolean isSub = event.getUser().isSubscriber();
        
        ChatMessage msg = new ChatMessage(
            user, 
            text, 
            generateColor(user), 
            new HashMap<>(), // badges
            isSub, 
            isMod, 
            false, // vip
            false  // broadcaster (difficult to detect strictly from comment event sometimes)
        );
        
        overlay.addMessage(msg);
    }
    
    private void handleGift(TikTokGiftEvent event) {
        String user = event.getUser().getProfileName();
        String giftName = event.getGift().getName();
        int count = event.getCombo();
        
        String text = "sent " + count + "x " + giftName + "!";
        
        ChatMessage msg = new ChatMessage(
            user, 
            text, 
            Color.PINK, // Distinct color for gifts
            new HashMap<>(),
            event.getUser().isSubscriber(),
            event.getUser().isModerator(),
            false,
            false
        );
        
        overlay.addMessage(msg);
    }
    
    private void handleFollow(TikTokFollowEvent event) {
        // Optional: Show follows in chat? Maybe too spammy for big streams.
        // For now, let's skip or maybe add a small system msg if needed.
    }
    
    private void handleSubscribe(TikTokSubscribeEvent event) {
        String user = event.getUser().getProfileName();
        ChatMessage msg = new ChatMessage(
            "System", 
            user + " subscribed to the TikTok LIVE!", 
            Color.CYAN, 
            new HashMap<>(), 
            false, false, false, false
        );
        overlay.addMessage(msg);
    }
    
    private void addSystemMessage(String text) {
        ChatMessage msg = new ChatMessage("System", text, Color.GRAY, new HashMap<>(), false, false, false, false);
        overlay.addMessage(msg);
    }
    
    private Color generateColor(String name) {
        int hash = name.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        return new Color(r, g, b).brighter();
    }
}
