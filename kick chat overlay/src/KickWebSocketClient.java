import com.google.gson.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

class KickWebSocketClient extends WebSocketClient {
    private OverlayWindow overlay;
    private JCheckBox soundCheck;
    private String channelName;
    private String authToken;
    private Random random = new Random();
    private ScheduledExecutorService reconnectExecutor;
    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT_ATTEMPTS = 10;
    private final long INITIAL_RECONNECT_DELAY = 1000;
    private final long MAX_RECONNECT_DELAY = 30000;
    private boolean manualDisconnect = false;
    
    public KickWebSocketClient(String channel, OverlayWindow overlay, JCheckBox soundCheck, String authToken) throws Exception {
        super(new URI("wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=java&version=1.0.0"));
        this.channelName = channel.toLowerCase().trim();
        this.overlay = overlay;
        this.soundCheck = soundCheck;
        this.authToken = authToken;
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Set connection timeout
        setConnectionLostTimeout(30);
        
        // Add authentication headers if token is provided
        if (authToken != null && !authToken.trim().isEmpty()) {
            addHeader("Authorization", "Bearer " + authToken.trim());
        }
    }
    
    public void onOpen(ServerHandshake handshake) {
        System.out.println("WebSocket Connected to Kick");
        reconnectAttempts = 0;
        
        // Subscribe to chat events
        JsonObject subscribeMsg = new JsonObject();
        subscribeMsg.addProperty("event", "pusher:subscribe");
        
        JsonObject data = new JsonObject();
        data.addProperty("channel", "chatrooms." + channelName + ".v2");
        
        // Add authentication if token is provided
        // if (authToken != null && !authToken.trim().isEmpty()) {
        //     data.addProperty("auth", authToken);
        // }
        
        subscribeMsg.add("data", data);
        send(subscribeMsg.toString());
        
        System.out.println("Subscribed to channel: " + channelName);
    }
    
    public void onMessage(String message) {
        System.out.println("KickWebSocketClient: Received raw message: " + message);
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String event = json.get("event").getAsString();
            System.out.println("KickWebSocketClient: Parsed event: " + event);
            
            // Handle different Kick chat events
            if (event.equals("pusher:connection_established")) {
                System.out.println("Connection established with Pusher");
            } else if (event.equals("pusher_internal:subscription_succeeded")) {
                System.out.println("Subscription succeeded for channel: " + channelName);
            } else if (event.equals("pusher:ping")) {
                send("{\"event\":\"pusher:pong\"}");
                System.out.println("KickWebSocketClient: Sent pong response.");
            } else if (event.startsWith("App\\Events")) { // Handle all App\\Events first
                if (event.equals("App\\Events\\ChatMessageEvent")) {
                    handleChatMessage(json);
                } else {
                    System.out.println("KickWebSocketClient: Received other App Event: " + event);
                }
            } else {
                System.out.println("KickWebSocketClient: Unhandled event: " + event);
            }
        } catch (Exception e) {
            System.err.println("Error processing message in onMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleChatMessage(JsonObject json) {
        try {
            JsonObject data = JsonParser.parseString(json.get("data").getAsString()).getAsJsonObject();
            JsonObject sender = data.get("sender").getAsJsonObject();
            
            String username = sender.get("username").getAsString();
            String content = data.get("content").getAsString();
            
            // Extract user badges and roles
            Map<String, String> badges = extractBadges(sender);
            boolean isSubscriber = data.has("subscription") && data.get("subscription").getAsJsonObject().has("is_active");
            boolean isModerator = sender.has("is_moderator") && sender.get("is_moderator").getAsBoolean();
            boolean isVIP = sender.has("is_vip") && sender.get("is_vip").getAsBoolean();
            boolean isBroadcaster = sender.has("is_broadcaster") && sender.get("is_broadcaster").getAsBoolean();
            
            // Generate color based on user role with priority
            Color color = generateColorForUser(username, isBroadcaster, isModerator, isVIP, isSubscriber);
            
            ChatMessage msg = new ChatMessage(username, content, color, badges, 
                isSubscriber, isModerator, isVIP, isBroadcaster);
            System.out.println("KickWebSocketClient: Calling overlay.addMessage() for: " + content);
            overlay.addMessage(msg);
            
        } catch (Exception e) {
            System.err.println("Error processing chat message: " + e.getMessage());
        }
    }
    
    private Map<String, String> extractBadges(JsonObject sender) {
        Map<String, String> badges = new HashMap<>();
        
        try {
            if (sender.has("identity") && sender.get("identity").getAsJsonObject().has("badges")) {
                JsonArray badgeArray = sender.get("identity").getAsJsonObject().get("badges").getAsJsonArray();
                for (JsonElement badgeElement : badgeArray) {
                    JsonObject badge = badgeElement.getAsJsonObject();
                    if (badge.has("type") && badge.has("text")) {
                        badges.put(badge.get("type").getAsString(), badge.get("text").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting badges: " + e.getMessage());
        }
        
        return badges;
    }
    
    private Color generateColorForUser(String username, boolean isBroadcaster, boolean isModerator, boolean isVIP, boolean isSubscriber) {
        // Priority-based color assignment
        if (isBroadcaster) {
            return new Color(255, 0, 0); // Red for broadcaster
        } else if (isModerator) {
            return new Color(0, 255, 0); // Green for moderators
        } else if (isVIP) {
            return new Color(255, 215, 0); // Gold for VIPs
        } else if (isSubscriber) {
            return new Color(100, 149, 237); // Cornflower blue for subscribers
        }
        
        // Default: generate color from username hash
        int hash = username.hashCode();
        random.setSeed(hash);
        
        int r = 100 + random.nextInt(156);
        int g = 100 + random.nextInt(156);
        int b = 100 + random.nextInt(156);
        
        return new Color(r, g, b);
    }
    
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket Closed: " + reason + " (code: " + code + ", remote: " + remote + ")");
        if (!manualDisconnect && code != 1000) { // Only reconnect if not manually disconnected and not a normal closure
            scheduleReconnection();
        }
    }
    
    public void onError(Exception ex) {
        System.err.println("WebSocket Error: " + ex.getMessage());
        ex.printStackTrace();
    }
    
    private void scheduleReconnection() {
        long delay = calculateReconnectDelay();
        reconnectAttempts++;
        
        System.out.println("Scheduling reconnection attempt " + reconnectAttempts + " in " + delay + "ms");
        
        reconnectExecutor.schedule(() -> {
            try {
                System.out.println("Attempting to reconnect...");
                reconnect();
            } catch (Exception e) {
                System.err.println("Reconnection failed: " + e.getMessage());
                scheduleReconnection();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    private long calculateReconnectDelay() {
        // Exponential backoff with jitter
        long baseDelay = INITIAL_RECONNECT_DELAY * (long)Math.pow(2, reconnectAttempts);
        long jitter = (long)(Math.random() * 1000);
        return Math.min(baseDelay + jitter, MAX_RECONNECT_DELAY);
    }
    
    @Override
    public void close() {
        super.close();
    }

    public void disconnectGracefully() {
        if (isOpen()) {
            System.out.println("KickWebSocketClient: Disconnecting gracefully...");
            manualDisconnect = true;
            close();
        }
    }
    
    public boolean isConnected() {
        return getReadyState() == org.java_websocket.enums.ReadyState.OPEN;
    }
    
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
}