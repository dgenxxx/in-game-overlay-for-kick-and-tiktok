import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

class ControlPanel extends JFrame {
    private static class KickConnectionInfo {
        final String channelName;
        final String chatroomId;
        final String websocketUrl;
        final String clientId;
        final String authToken;

        KickConnectionInfo(String channelName, String chatroomId, String websocketUrl, String clientId,
                           String authToken) {
            this.channelName = channelName;
            this.chatroomId = chatroomId;
            this.websocketUrl = websocketUrl;
            this.clientId = clientId;
            this.authToken = authToken;
        }
    }

    private JTextField channelField;
    private JTextField tiktokField;
    private JTextField authTokenField;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JButton testBtn;
    private JButton colorsBtn;
    private JSlider opacitySlider;
    private JCheckBox soundCheck;
    private JCheckBox lockCheck;
    private JCheckBox visibleCheck;
    private OverlayWindow overlay;
    private KickWebSocketClient wsClient;
    private TikTokClient tiktokClient;
    private JLabel statusLabel = new JLabel("Disconnected");
    private ConfigManager config;
    private JSpinner fadeTimeSpinner;
    
    public ControlPanel() {
        setTitle("Kick Chat Overlay Control Panel");
        setSize(700, 500);  // Increased from 600x450 for better readability
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.out.println("ControlPanel: Window closing event received.");
                if (wsClient != null) {
                    wsClient.disconnectGracefully(); // Ensure graceful disconnection
                }
                System.exit(0);
            }
        });
        setLocationRelativeTo(null);
        
        initComponents();
        layoutComponents();
    }
    
    private void initComponents() {
        config = new ConfigManager();
        int initialTransparency = config.getInt("transparency", 15);
        int initialFadeTime = config.getInt("fadeTime", 30);
        
        channelField = new JTextField("6922175", 15);
        tiktokField = new JTextField(15);
        authTokenField = new JTextField(15);
        authTokenField.setToolTipText("Optional: Authentication token for private channels");
        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");
        disconnectBtn.setEnabled(false);
        testBtn = new JButton("Test Overlay");
        colorsBtn = new JButton("Colors");
        
        // Initialize overlay immediately
        if (overlay == null) {
            overlay = new OverlayWindow(true, config); // Default sound enabled, pass config
            overlay.setVisible(true);
            overlay.setWindowTransparency(initialTransparency);
            overlay.setFadeTime(initialFadeTime);
            overlay.setLocked(true);
            overlay.setWindowVisible(true);
        }
        
        opacitySlider = new JSlider(0, 100, initialTransparency);
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        
        fadeTimeSpinner = new JSpinner(new SpinnerNumberModel(initialFadeTime, 5, 300, 5));
        
        soundCheck = new JCheckBox("Sound Effects", true);
        lockCheck = new JCheckBox("Lock Overlay (Click-Through)", true);
        visibleCheck = new JCheckBox("Visible Window", false);
        
        connectBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });
        
        // Use MouseListener instead of ActionListener to prevent phantom key triggers
        disconnectBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (disconnectBtn.isEnabled()) {
                    if (wsClient != null) {
                        wsClient.disconnectGracefully();
                    }
                    disconnect();
                }
            }
        });
        
        testBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (overlay != null) {
                    overlay.addTestMessage();
                }
            }
        });
        
        colorsBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showColorDialog();
            }
        });
        
        opacitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                if (overlay != null) {
                    int val = opacitySlider.getValue();
                    overlay.setWindowTransparency(val);
                    config.setInt("transparency", val);
                    config.save();
                }
            }
        });
        
        fadeTimeSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                if (overlay != null) {
                    int val = (Integer) fadeTimeSpinner.getValue();
                    overlay.setFadeTime(val);
                    config.setInt("fadeTime", val);
                    config.save();
                }
            }
        });
        
        lockCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (overlay != null) {
                    overlay.setLocked(lockCheck.isSelected());
                }
            }
        });
        
        visibleCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (overlay != null) {
                    overlay.setBorderVisible(visibleCheck.isSelected());
                }
            }
        });
        
        // Apply initial state from checkbox on startup
        if (overlay != null) {
            overlay.setBorderVisible(visibleCheck.isSelected());
        }
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Kick username or chatroom ID:"));
        inputPanel.add(channelField);
        
        JPanel tiktokPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tiktokPanel.add(new JLabel("TikTok User:"));
        tiktokPanel.add(tiktokField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(connectBtn);
        buttonPanel.add(disconnectBtn);
        buttonPanel.add(testBtn);
        buttonPanel.add(colorsBtn);
        
        topPanel.add(inputPanel);
        topPanel.add(tiktokPanel);
        topPanel.add(buttonPanel);
        
        JPanel centerPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        
        JPanel opacityPanel = new JPanel(new BorderLayout());
        opacityPanel.add(new JLabel("Transparency:"), BorderLayout.WEST);
        opacityPanel.add(opacitySlider, BorderLayout.CENTER);
        
        JPanel fadePanel = new JPanel(new BorderLayout());
        fadePanel.add(new JLabel("Msg Fade Time (s):"), BorderLayout.WEST);
        fadePanel.add(fadeTimeSpinner, BorderLayout.CENTER);
        
        centerPanel.add(opacityPanel);
        centerPanel.add(fadePanel);
        centerPanel.add(soundCheck);
        centerPanel.add(lockCheck);
        centerPanel.add(visibleCheck);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Status: "));
        bottomPanel.add(statusLabel);
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void connect() {
        String channelInput = channelField.getText().trim();
        String tiktokInput = tiktokField.getText().trim();
        String authToken = authTokenField.getText().trim();
        
        if (channelInput.isEmpty() && tiktokInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Kick Channel ID or TikTok Username");
            return;
        }

        connectBtn.setEnabled(false);
        
        // If we have a Kick channel, resolve it
        if (!channelInput.isEmpty()) {
            statusLabel.setText("Resolving ID...");
            statusLabel.setForeground(Color.ORANGE);
            
            new SwingWorker<KickConnectionInfo, Void>() {
                @Override
                protected KickConnectionInfo doInBackground() throws Exception {
                    String chatroomId;
                    if (channelInput.matches("\\d+")) {
                        chatroomId = channelInput;
                    } else {
                        chatroomId = getChatroomId(channelInput);
                    }
                    if (chatroomId == null || !chatroomId.matches("\\d+")) {
                        throw new IOException("Kick did not return a numeric chatroom ID. Check the channel username or enter its numeric chatroom ID.");
                    }
                    String clientId = UUID.randomUUID().toString();
                    String websocketUrl = getKickWebSocketUrl(chatroomId, clientId, authToken);
                    return new KickConnectionInfo(channelInput, chatroomId, websocketUrl, clientId, authToken);
                }
                
                @Override
                protected void done() {
                    try {
                        KickConnectionInfo connectionInfo = get();
                        System.out.println("Resolved Kick chatroom ID: " + connectionInfo.chatroomId);
                        performConnection(connectionInfo);
                        
                    } catch (Exception e) {
                        Throwable cause = e.getCause() == null ? e : e.getCause();
                        String detail = cause.getMessage() == null ? cause.toString() : cause.getMessage();
                        showKickSetupError(channelInput, detail);
                        if (!tiktokInput.isEmpty()) {
                            performConnection(null);
                        } else {
                            connectBtn.setEnabled(true);
                        }
                    }
                }
            }.execute();
        } else {
            // No Kick channel, connect directly (TikTok only)
            performConnection(null);
        }
    }

    private void performConnection(KickConnectionInfo connectionInfo) {
        try {
            overlay.setWindowVisible(visibleCheck.isSelected());
            
            // Update overlay settings just in case
            overlay.setWindowTransparency(opacitySlider.getValue());
            
            // Connect to Kick (only if channelId is provided)
            if (connectionInfo != null) {
                wsClient = new KickWebSocketClient(connectionInfo.channelName, connectionInfo.chatroomId,
                    connectionInfo.websocketUrl, connectionInfo.clientId, overlay, soundCheck,
                    connectionInfo.authToken);
                wsClient.connect();
            }
            
            // Connect to TikTok if provided
            String tiktokUser = tiktokField.getText().trim();
            if (!tiktokUser.isEmpty()) {
                try {
                    // Use reflection to check if TikTok class is available to avoid crash if jars are missing
                    Class.forName("io.github.jwdeveloper.tiktok.TikTokLive");
                    tiktokClient = new TikTokClient(tiktokUser, overlay);
                    tiktokClient.connect();
                } catch (ClassNotFoundException e) {
                    System.out.println("TikTok library not found. Skipping TikTok connection.");
                    JOptionPane.showMessageDialog(this, 
                        "TikTok library not installed.\n" +
                        "Please download 'client.jar' (TikTokLiveJava) and 'protobuf-java.jar'\n" +
                        "and place them in the 'lib' folder to enable TikTok support.", 
                        "TikTok Support Missing", JOptionPane.WARNING_MESSAGE);
                }
            }
            
            // connectBtn is already disabled
            disconnectBtn.setEnabled(true);
            channelField.setEnabled(false);
            tiktokField.setEnabled(false);
            
            StringBuilder status = new StringBuilder("Connected: ");
            boolean hasKick = connectionInfo != null;
            boolean hasTikTok = !tiktokField.getText().trim().isEmpty();
            
            if (hasKick) status.append("Kick");
            if (hasKick && hasTikTok) status.append(" & ");
            if (hasTikTok) status.append("TikTok");
            
            statusLabel.setText(status.toString());
            statusLabel.setForeground(Color.GREEN);
            
            // Move focus away from buttons to prevent accidental triggering
            this.requestFocusInWindow();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error connecting: " + ex.getMessage());
            ex.printStackTrace();
            System.out.println("Connection error: " + ex.getMessage());
            connectBtn.setEnabled(true);
            statusLabel.setText("Connection Error");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    private void disconnect() {
        if (wsClient != null) {
            wsClient.close();
            wsClient = null;
        }
        
        if (tiktokClient != null) {
            tiktokClient.disconnect();
            tiktokClient = null;
        }
        
        if (overlay != null) {
            overlay.clearMessages();
        }
        
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        channelField.setEnabled(true);
        tiktokField.setEnabled(true);
        statusLabel.setText("Not connected");
        statusLabel.setForeground(Color.BLACK);
    }

    private String getChatroomId(String username) {
        HttpURLConnection conn = null;
        try {
            String encodedUsername = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8.name());
            URL url = new URL("https://kick.com/api/v2/channels/" + encodedUsername);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            
            if (conn.getResponseCode() == 200) {
                String response;
                try (Scanner scanner = new Scanner(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8.name())) {
                    response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
                
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                if (json.has("chatroom") && json.get("chatroom").isJsonObject()) {
                    JsonObject chatroom = json.getAsJsonObject("chatroom");
                    if (chatroom.has("id") && !chatroom.get("id").isJsonNull()) {
                        return chatroom.get("id").getAsString();
                    }
                }
            } else {
                System.out.println("API Request failed: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.out.println("Error fetching chatroom ID: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private String getKickWebSocketUrl(String chatroomId, String clientId, String authToken) throws Exception {
        String endpoint = "https://web.kick.com/api/v1/realtime/channels/" + chatroomId + "/chat/connection";
        JsonObject client = new JsonObject();
        client.addProperty("id", clientId);
        client.addProperty("type", "web");

        JsonObject pusher = new JsonObject();
        pusher.addProperty("provider", "pusher");
        JsonObject centrifugo = new JsonObject();
        centrifugo.addProperty("provider", "centrifugo");
        JsonArray providers = new JsonArray();
        providers.add(pusher);
        providers.add(centrifugo);

        JsonObject capabilities = new JsonObject();
        capabilities.add("accepted_providers", providers);
        JsonObject payload = new JsonObject();
        payload.add("client", client);
        payload.add("capabilities", capabilities);

        JsonObject response = postKickJson(endpoint, payload.toString(), authToken);
        JsonObject data = getJsonObject(response.get("data"));
        JsonArray connections = data != null && data.has("connections") && data.get("connections").isJsonArray()
            ? data.getAsJsonArray("connections") : null;
        if (connections != null) {
            for (com.google.gson.JsonElement element : connections) {
                JsonObject connection = getJsonObject(element);
                if (connection == null || !"centrifugo".equalsIgnoreCase(getJsonString(connection, "provider"))) {
                    continue;
                }
                JsonObject credentials = getJsonObject(connection.get("credentials"));
                String websocketUrl = getJsonString(credentials, "url");
                if (websocketUrl != null && websocketUrl.startsWith("wss://")) {
                    return websocketUrl;
                }
            }
        }
        throw new IOException("Kick did not provide a Centrifugo chat connection for this chatroom.");
    }

    private JsonObject postKickJson(String endpoint, String requestBody, String authToken) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setUseCaches(false);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Origin", "https://kick.com");
            conn.setRequestProperty("Referer", "https://kick.com/");
            conn.setRequestProperty("x-app-platform", "web");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            if (authToken != null && !authToken.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken.trim());
            }

            byte[] body = requestBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = conn.getOutputStream()) {
                output.write(body);
            }

            int status = conn.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = "";
            if (stream != null) {
                try (Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name())) {
                    responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
            }
            if (status < 200 || status >= 300) {
                String message = null;
                try {
                    JsonObject error = JsonParser.parseString(responseBody).getAsJsonObject();
                    message = getJsonString(error, "message");
                } catch (Exception ignored) {
                    // Avoid surfacing non-JSON proxy or server pages in the UI.
                }
                throw new IOException("Kick realtime API returned HTTP " + status +
                    (message == null || message.trim().isEmpty() ? "" : ": " + message));
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private JsonObject getJsonObject(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            try {
                com.google.gson.JsonElement parsed = JsonParser.parseString(element.getAsString());
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String getJsonString(JsonObject object, String key) {
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

    private void showKickSetupError(String channelInput, String detail) {
        connectBtn.setEnabled(true);
        statusLabel.setText("Could not connect to Kick");
        statusLabel.setForeground(Color.RED);
        JOptionPane.showMessageDialog(ControlPanel.this,
            "Could not set up Kick chat for '" + channelInput + "'.\n\n" + detail,
            "Kick Connection Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showColorDialog() {
        if (overlay == null) return;
        
        JDialog dialog = new JDialog(this, "Role Colors", true);
        dialog.setLayout(new GridLayout(5, 1, 10, 10));
        dialog.setSize(300, 300);
        dialog.setLocationRelativeTo(this);
        
        String[] roles = {"Broadcaster", "Moderator", "VIP", "Subscriber", "Default"};
        
        for (final String role : roles) {
            JButton btn = new JButton("Set " + role + " Color");
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Color newColor = JColorChooser.showDialog(dialog, "Choose " + role + " Color", Color.WHITE);
                    if (newColor != null) {
                        overlay.setRoleColor(role, newColor);
                    }
                }
            });
            dialog.add(btn);
        }
        
        dialog.setVisible(true);
    }
}
