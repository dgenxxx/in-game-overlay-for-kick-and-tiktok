import com.google.gson.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class KickWebSocketClient extends WebSocketClient {
    private static final String WEB_API = "https://web.kick.com/api/v1/realtime";
    private static final String CHAT_MESSAGE_EVENT = "App\\Events\\ChatMessageEvent";

    private final OverlayWindow overlay;
    private final JCheckBox soundCheck;
    private final String channelName;
    private final String chatroomId;
    private final String websocketUrl;
    private final String clientId;
    private final String authToken;
    private final Random random = new Random();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService authExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService viewerCountExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean connectionErrorShown = new AtomicBoolean(false);
    private final AtomicLong connectionGeneration = new AtomicLong(0);

    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT_ATTEMPTS = 10;
    private final long INITIAL_RECONNECT_DELAY = 1000;
    private final long MAX_RECONNECT_DELAY = 30000;
    private volatile boolean manualDisconnect = false;
    private volatile boolean stopReconnecting = false;
    private volatile boolean suppressCloseMessage = false;
    private volatile boolean hasConnectedOnce = false;
    private volatile boolean connectionCommandSent = false;
    private volatile boolean subscriptionSent = false;
    private ScheduledFuture<?> viewerCountTask;

    public KickWebSocketClient(String channel, String chatroomId, String websocketUrl, String clientId,
                               OverlayWindow overlay, JCheckBox soundCheck, String authToken) throws Exception {
        super(new URI(websocketUrl));
        this.channelName = channel.toLowerCase(Locale.ROOT).trim();
        if (chatroomId == null || !chatroomId.trim().matches("\\d+")) {
            throw new IllegalArgumentException("Kick chatroom ID must be numeric");
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Kick realtime client ID is missing");
        }
        this.chatroomId = chatroomId.trim();
        this.websocketUrl = websocketUrl;
        this.clientId = clientId.trim();
        this.overlay = overlay;
        this.soundCheck = soundCheck;
        this.authToken = authToken == null ? "" : authToken.trim();
        setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("KickWebSocketClient: WebSocket transport opened; requesting a connection token.");
        connectionCommandSent = false;
        subscriptionSent = false;
        long generation = connectionGeneration.incrementAndGet();

        // Fetch a fresh short-lived token away from the WebSocket callback thread.
        authExecutor.execute(() -> {
            try {
                String token = requestConnectionToken();
                if (generation == connectionGeneration.get() && isOpen() && !stopReconnecting) {
                    sendConnectionCommand(token);
                }
            } catch (Exception e) {
                if (generation != connectionGeneration.get()) {
                    return;
                }
                System.err.println("KickWebSocketClient: Could not obtain a realtime token: " + e.getMessage());
                showConnectionError("Could not authenticate with Kick: " + e.getMessage());
                stopAfterProtocolError();
            }
        });
    }

    private String requestConnectionToken() throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("client_id", clientId);
        JsonObject response = postKickJson(WEB_API + "/auth/connection", payload.toString());
        JsonObject data = getObject(response.get("data"));
        String token = getString(data, "token");
        if (token == null || token.trim().isEmpty()) {
            token = getString(response, "token");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Kick returned no connection token");
        }
        return token;
    }

    private JsonObject postKickJson(String endpoint, String requestBody) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Origin", "https://kick.com");
            connection.setRequestProperty("Referer", "https://kick.com/");
            connection.setRequestProperty("x-app-platform", "web");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            if (!authToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            byte[] body = requestBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            InputStream responseStream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = "";
            if (responseStream != null) {
                try (Scanner scanner = new Scanner(responseStream, StandardCharsets.UTF_8.name())) {
                    responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
            }
            if (status < 200 || status >= 300) {
                String detail = "";
                try {
                    JsonObject error = JsonParser.parseString(responseBody).getAsJsonObject();
                    detail = getString(error, "message");
                } catch (Exception ignored) {
                    // Keep the public error concise if the service returns non-JSON content.
                }
                throw new IllegalStateException("Kick realtime API returned HTTP " + status +
                    (detail == null || detail.isEmpty() ? "" : ": " + detail));
            }
            JsonElement parsed = JsonParser.parseString(responseBody);
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("Kick returned an unreadable realtime response");
            }
            return parsed.getAsJsonObject();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private synchronized void sendConnectionCommand(String token) {
        if (connectionCommandSent || !isOpen() || stopReconnecting) {
            return;
        }
        JsonObject connect = new JsonObject();
        connect.addProperty("token", token);
        connect.addProperty("name", "java");
        connect.addProperty("version", "1.0.0");
        JsonObject command = new JsonObject();
        command.addProperty("id", 1);
        command.add("connect", connect);
        send(command.toString());
        connectionCommandSent = true;
        System.out.println("KickWebSocketClient: Sent Centrifugo connect command.");
    }

    @Override
    public void onMessage(String message) {
        // Centrifugo may batch multiple JSON protocol messages in one text frame.
        for (String frame : message.split("\\n")) {
            if (frame.trim().isEmpty()) {
                continue;
            }
            try {
                JsonElement parsed = JsonParser.parseString(frame);
                if (!parsed.isJsonObject()) {
                    continue;
                }
                handleProtocolMessage(parsed.getAsJsonObject());
            } catch (Exception e) {
                System.err.println("KickWebSocketClient: Could not process realtime frame: " + e.getMessage());
            }
        }
    }

    private void handleProtocolMessage(JsonObject json) {
        if (json.entrySet().isEmpty()) {
            // Centrifugo's JSON heartbeat is an empty object; send an empty pong command.
            if (isOpen()) {
                send("{}");
            }
            return;
        }

        JsonObject error = getObject(json.get("error"));
        if (error != null) {
            String detail = getString(error, "message");
            if (detail == null || detail.trim().isEmpty()) {
                detail = "Kick rejected the realtime connection or chat subscription.";
            }
            showConnectionError("Kick connection error: " + detail);
            stopAfterProtocolError();
            return;
        }

        if (json.has("connect") && !json.get("connect").isJsonNull()) {
            reconnectAttempts = 0;
            subscribeToChat();
            if (!hasConnectedOnce) {
                hasConnectedOnce = true;
                ChatMessage connectMsg = new ChatMessage("System", "✓ Connected to Kick: " + channelName,
                    Color.GRAY, new HashMap<>(), false, false, false, false, "System");
                overlay.addMessage(connectMsg);
            } else {
                System.out.println("KickWebSocketClient: Reconnected to Kick (message suppressed).");
            }
            startViewerCountUpdates();
            return;
        }

        if (json.has("subscribe") && !json.get("subscribe").isJsonNull()) {
            System.out.println("KickWebSocketClient: Subscribed to chatrooms." + chatroomId + ".v2");
            return;
        }

        JsonObject push = getObject(json.get("push"));
        if (push != null) {
            handlePush(push);
        }
    }

    private synchronized void subscribeToChat() {
        if (subscriptionSent || !isOpen() || stopReconnecting) {
            return;
        }
        JsonObject subscription = new JsonObject();
        subscription.addProperty("channel", "chatrooms." + chatroomId + ".v2");
        JsonObject command = new JsonObject();
        command.addProperty("id", 2);
        command.add("subscribe", subscription);
        send(command.toString());
        subscriptionSent = true;
        System.out.println("KickWebSocketClient: Requested chatrooms." + chatroomId + ".v2");
    }

    private void handlePush(JsonObject push) {
        JsonObject publication = getObject(push.get("pub"));
        if (publication == null) {
            return;
        }
        JsonObject envelope = getObject(publication.get("data"));
        if (envelope == null) {
            return;
        }
        String event = getString(envelope, "event");
        if (CHAT_MESSAGE_EVENT.equals(event)) {
            JsonObject data = getObject(envelope.get("data"));
            if (data != null) {
                handleChatMessage(data);
            }
        }
    }

    private void showConnectionError(String message) {
        if (connectionErrorShown.compareAndSet(false, true)) {
            ChatMessage errorMsg = new ChatMessage("System", message,
                Color.ORANGE, new HashMap<>(), false, false, false, false, "System");
            overlay.addMessage(errorMsg);
        }
    }

    private void stopAfterProtocolError() {
        stopReconnecting = true;
        suppressCloseMessage = true;
        stopViewerCountUpdates();
        reconnectExecutor.shutdownNow();
        authExecutor.shutdownNow();
        viewerCountExecutor.shutdownNow();
        close();
    }

    private void handleChatMessage(JsonObject data) {
        try {
            JsonObject sender = getObject(data.get("sender"));
            if (sender == null) {
                System.err.println("KickWebSocketClient: Chat event had no sender object.");
                return;
            }

            String username = getString(sender, "username");
            if (username == null || username.trim().isEmpty()) {
                username = getString(sender, "slug");
            }
            String content = getString(data, "content");
            if (username == null || username.trim().isEmpty() || content == null) {
                System.err.println("KickWebSocketClient: Skipping chat event without a username or message content.");
                return;
            }

            Map<String, String> badges = extractBadges(sender);
            JsonObject subscription = getObject(data.get("subscription"));
            boolean isSubscriber = subscription != null &&
                (!subscription.has("is_active") || getBoolean(subscription, "is_active"));
            isSubscriber = isSubscriber || hasBadge(badges, "subscriber");
            boolean isModerator = getBoolean(sender, "is_moderator") || hasBadge(badges, "moderator");
            boolean isVIP = getBoolean(sender, "is_vip") || hasBadge(badges, "vip");
            boolean isBroadcaster = getBoolean(sender, "is_broadcaster") || hasBadge(badges, "broadcaster");

            Color color = generateColorForUser(username, isBroadcaster, isModerator, isVIP, isSubscriber);
            ChatMessage msg = new ChatMessage(username, content, color, badges,
                isSubscriber, isModerator, isVIP, isBroadcaster, "Kick");
            overlay.addMessage(msg);
            overlay.addKickViewer(username);
        } catch (Exception e) {
            System.err.println("Error processing Kick chat message: " + e.getMessage());
        }
    }

    private Map<String, String> extractBadges(JsonObject sender) {
        Map<String, String> badges = new HashMap<>();
        JsonObject identity = getObject(sender.get("identity"));
        if (identity != null && identity.has("badges") && identity.get("badges").isJsonArray()) {
            for (JsonElement badgeElement : identity.getAsJsonArray("badges")) {
                if (!badgeElement.isJsonObject()) {
                    continue;
                }
                JsonObject badge = badgeElement.getAsJsonObject();
                String type = getString(badge, "type");
                String text = getString(badge, "text");
                if (type != null && text != null) {
                    badges.put(type, text);
                }
            }
        }
        return badges;
    }

    private JsonObject getObject(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            try {
                JsonElement parsed = JsonParser.parseString(element.getAsString());
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull() ||
                !object.get(key).isJsonPrimitive()) {
            return null;
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean getBoolean(JsonObject object, String key) {
        String value = getString(object, key);
        return value != null && Boolean.parseBoolean(value);
    }

    private boolean hasBadge(Map<String, String> badges, String role) {
        for (Map.Entry<String, String> badge : badges.entrySet()) {
            if (badge.getKey().toLowerCase(Locale.ROOT).contains(role) ||
                    badge.getValue().toLowerCase(Locale.ROOT).contains(role)) {
                return true;
            }
        }
        return false;
    }

    private Color generateColorForUser(String username, boolean isBroadcaster, boolean isModerator,
                                       boolean isVIP, boolean isSubscriber) {
        if (isBroadcaster) {
            return new Color(255, 0, 0);
        } else if (isModerator) {
            return new Color(0, 255, 0);
        } else if (isVIP) {
            return new Color(255, 215, 0);
        } else if (isSubscriber) {
            return new Color(100, 149, 237);
        }
        random.setSeed(username.hashCode());
        return new Color(100 + random.nextInt(156), 100 + random.nextInt(156), 100 + random.nextInt(156));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Kick WebSocket closed: " + reason + " (code: " + code + ", remote: " + remote + ")");
        connectionGeneration.incrementAndGet();
        stopViewerCountUpdates();
        if ((manualDisconnect || code == 1000) && !suppressCloseMessage) {
            ChatMessage disconnectMsg = new ChatMessage("System", "Disconnected from Kick",
                Color.GRAY, new HashMap<>(), false, false, false, false, "System");
            overlay.addMessage(disconnectMsg);
        }
        if (!manualDisconnect && !stopReconnecting && code != 1000) {
            scheduleReconnection();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Kick WebSocket error: " + (ex == null ? "unknown error" : ex.getMessage()));
    }

    private void scheduleReconnection() {
        if (stopReconnecting || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS || reconnectExecutor.isShutdown()) {
            System.out.println("KickWebSocketClient: Reconnection stopped.");
            return;
        }
        long delay = calculateReconnectDelay();
        reconnectAttempts++;
        System.out.println("KickWebSocketClient: Scheduling reconnect " + reconnectAttempts + " in " + delay + "ms");
        reconnectExecutor.schedule(() -> {
            if (stopReconnecting || manualDisconnect) {
                return;
            }
            try {
                reconnect();
            } catch (Exception e) {
                System.err.println("KickWebSocketClient: Reconnect failed: " + e.getMessage());
                scheduleReconnection();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private long calculateReconnectDelay() {
        long baseDelay = INITIAL_RECONNECT_DELAY * (long) Math.pow(2, reconnectAttempts);
        long jitter = (long) (Math.random() * 1000);
        return Math.min(baseDelay + jitter, MAX_RECONNECT_DELAY);
    }

    public void disconnectGracefully() {
        manualDisconnect = true;
        stopReconnecting = true;
        stopViewerCountUpdates();
        reconnectExecutor.shutdownNow();
        authExecutor.shutdownNow();
        viewerCountExecutor.shutdownNow();
        if (getReadyState() != org.java_websocket.enums.ReadyState.CLOSED) {
            close();
        }
        overlay.clearKickViewers();
    }

    private synchronized void startViewerCountUpdates() {
        if (channelName.matches("\\d+") || viewerCountExecutor.isShutdown() ||
                (viewerCountTask != null && !viewerCountTask.isCancelled())) {
            return;
        }
        viewerCountTask = viewerCountExecutor.scheduleAtFixedRate(this::fetchViewerCount, 0, 10, TimeUnit.SECONDS);
    }

    private void stopViewerCountUpdates() {
        if (viewerCountTask != null) {
            viewerCountTask.cancel(false);
            viewerCountTask = null;
        }
    }

    private void fetchViewerCount() {
        try {
            String url = "https://kick.com/api/v2/channels/" + channelName;
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .GET()
                .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("livestream") && !json.get("livestream").isJsonNull()) {
                    JsonObject livestream = json.getAsJsonObject("livestream");
                    if (livestream.has("viewer_count")) {
                        overlay.updateKickViewerCount(livestream.get("viewer_count").getAsInt());
                    }
                } else {
                    overlay.updateKickViewerCount(0);
                }
            }
        } catch (Exception e) {
            System.err.println("KickWebSocketClient: Error fetching viewer count: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return getReadyState() == org.java_websocket.enums.ReadyState.OPEN;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
}
