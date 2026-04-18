import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;

public class TestTikTok {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java TestTikTok <tiktok_username>");
            System.out.println("Example: java TestTikTok username");
            return;
        }
        
        String username = args[0];
        System.out.println("Testing TikTok connection to: " + username);
        System.out.println("TikTok library loaded successfully!");
        
        try {
            System.out.println("Creating TikTok client...");
            LiveClient client = TikTokLive.newClient(username)
                .onConnected((liveClient, event) -> {
                    System.out.println("✓ CONNECTED to @" + username);
                    System.out.println("Waiting for messages...");
                })
                .onDisconnected((liveClient, event) -> {
                    System.out.println("✗ DISCONNECTED");
                })
                .onError((liveClient, event) -> {
                    System.err.println("ERROR: " + event.getException().getMessage());
                    event.getException().printStackTrace();
                })
                .onComment((liveClient, event) -> {
                    System.out.println("💬 " + event.getUser().getName() + ": " + event.getText());
                })
                .buildAndConnect();
            
            System.out.println("Client created, connecting...");
            
            // Keep alive for 60 seconds
            Thread.sleep(60000);
            
            System.out.println("Test complete, disconnecting...");
            client.disconnect();
            
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
