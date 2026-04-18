import java.awt.*;
import java.util.*;

class ChatMessage {
    String username;
    String message;
    Color color;
    long timestamp;
    Map<String, String> badges;
    boolean isSubscriber;
    boolean isModerator;
    boolean isVIP;
    boolean isBroadcaster;
    
    public ChatMessage(String username, String message, Color color, Map<String, String> badges, 
                      boolean isSubscriber, boolean isModerator, boolean isVIP, boolean isBroadcaster) {
        this.username = username;
        this.message = message;
        this.color = color;
        this.timestamp = System.currentTimeMillis();
        this.badges = badges != null ? badges : new HashMap<>();
        this.isSubscriber = isSubscriber;
        this.isModerator = isModerator;
        this.isVIP = isVIP;
        this.isBroadcaster = isBroadcaster;
    }
}