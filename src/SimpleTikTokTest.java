import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;

public class SimpleTikTokTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java SimpleTikTokTest <username>");
            return;
        }
        
        String username = args[0];
        System.out.println("===========================================");
        System.out.println("Testing TikTok Connection");
        System.out.println("Username: @" + username);
        System.out.println("===========================================");
        System.out.println();
        
        // First check if user is online
        System.out.println("[1] Checking if user is LIVE...");
        try {
            boolean isLive = TikTokLive.isLiveOnline(username);
            System.out.println("    Result: " + (isLive ? "USER IS LIVE ✓" : "USER IS OFFLINE ✗"));
            System.out.println();
            
            if (!isLive) {
                System.out.println("ERROR: User is not currently LIVE!");
                System.out.println("TikTok library can only connect to active streams.");
                System.out.println("Please try a different user who is streaming right now.");
                return;
            }
        } catch (Exception e) {
            System.out.println("    Could not check status: " + e.getMessage());
            System.out.println("    Continuing anyway...");
            System.out.println();
        }
        
        // Try to connect
        System.out.println("[2] Creating TikTok client...");
        LiveClient client = TikTokLive.newClient(username)
            .onConnected((liveClient, event) -> {
                System.out.println();
                System.out.println("===========================================");
                System.out.println("✓✓✓ CONNECTED SUCCESSFULLY! ✓✓✓");
                System.out.println("===========================================");
                System.out.println();
            })
            .onDisconnected((liveClient, event) -> {
                System.out.println();
                System.out.println("✗ Disconnected");
                System.out.println();
            })
            .onError((liveClient, event) -> {
                System.err.println();
                System.err.println("===========================================");
                System.err.println("ERROR OCCURRED:");
                System.err.println("Message: " + event.getException().getMessage());
                System.err.println("===========================================");
                event.getException().printStackTrace();
                System.err.println();
            })
            .onComment((liveClient, event) -> {
                String user = event.getUser().getName();
                String text = event.getText();
                System.out.println("[CHAT] " + user + ": " + text);
            })
            .buildAndConnect();
        
        System.out.println("[3] Client created: " + (client != null ? "YES" : "NO"));
        System.out.println("[4] Waiting 60 seconds for messages...");
        System.out.println("    (Press Ctrl+C to stop)");
        System.out.println();
        
        Thread.sleep(60000);
        
        System.out.println();
        System.out.println("[5] Disconnecting...");
        client.disconnect();
        System.out.println("[6] Test complete!");
    }
}
