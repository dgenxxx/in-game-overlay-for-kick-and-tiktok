import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;

public class MinimalTikTokTest {
    public static void main(String[] args) {
        System.out.println("=== Minimal TikTok Test ===");
        
        if (args.length == 0) {
            System.out.println("Usage: java MinimalTikTokTest <username>");
            System.out.println("Example: java MinimalTikTokTest testuser");
            return;
        }
        
        String username = args[0];
        System.out.println("Testing: @" + username);
        System.out.println();
        
        try {
            System.out.println("[1] Creating client...");
            LiveClient client = TikTokLive.newClient(username)
                .onConnected((liveClient, event) -> {
                    System.out.println("[SUCCESS] Connected!");
                })
                .onError((liveClient, event) -> {
                    System.err.println("[ERROR] " + event.getException().getMessage());
                    event.getException().printStackTrace();
                })
                .onComment((liveClient, event) -> {
                    System.out.println("[CHAT] " + event.getUser().getName() + ": " + event.getText());
                })
                .buildAndConnect();
            
            System.out.println("[2] Client created: " + (client != null));
            System.out.println("[3] Waiting 30 seconds for messages...");
            System.out.println();
            
            Thread.sleep(30000);
            
            System.out.println();
            System.out.println("[4] Disconnecting...");
            client.disconnect();
            System.out.println("[5] Done!");
            
        } catch (Exception e) {
            System.err.println("[FATAL ERROR]");
            System.err.println("Type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
