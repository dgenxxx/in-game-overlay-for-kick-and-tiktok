import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

class ControlPanel extends JFrame {
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
    private OverlayWindow overlay;
    private KickWebSocketClient wsClient;
    private TikTokClient tiktokClient;
    private JLabel statusLabel;
    private ConfigManager config;
    private JSpinner fadeTimeSpinner;
    
    public ControlPanel() {
        setTitle("Kick Chat Overlay Control Panel");
        setSize(500, 400);
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
        
        channelField = new JTextField(15);
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
        }
        
        opacitySlider = new JSlider(0, 100, initialTransparency);
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        
        fadeTimeSpinner = new JSpinner(new SpinnerNumberModel(initialFadeTime, 5, 300, 5));
        
        soundCheck = new JCheckBox("Sound Effects", true);
        lockCheck = new JCheckBox("Lock Overlay (Click-Through)", false);
        statusLabel = new JLabel("Not connected");
        
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
                    if (!overlay.isVisible()) {
                        overlay.setVisible(true);
                    }
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
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Kick Channel/ID:"));
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
        
        JPanel centerPanel = new JPanel(new GridLayout(5, 1, 5, 5));
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
        
        if (channelInput.isEmpty() && tiktokInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Kick Channel ID or TikTok Username");
            return;
        }

        connectBtn.setEnabled(false);
        
        // If we have a Kick channel, resolve it
        if (!channelInput.isEmpty()) {
            statusLabel.setText("Resolving ID...");
            statusLabel.setForeground(Color.ORANGE);
            
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    if (channelInput.matches("\\d+")) {
                        return channelInput; // Already an ID
                    }
                    return getChatroomId(channelInput);
                }
                
                @Override
                protected void done() {
                    try {
                        String chatroomId = get();
                        
                        if (chatroomId == null) {
                            System.out.println("Could not resolve Chatroom ID. Using username as fallback.");
                            chatroomId = channelInput;
                             JOptionPane.showMessageDialog(ControlPanel.this, 
                                "Could not auto-resolve Chatroom ID (likely due to Cloudflare).\n" +
                                "Kick messages may not appear.\n\n" +
                                "Please try entering your numeric Chatroom ID manually.\n" +
                                "(You can find it in the API response: https://kick.com/api/v2/channels/" + channelInput + ")",
                                "Warning", JOptionPane.WARNING_MESSAGE);
                        } else {
                             System.out.println("Resolved Chatroom ID: " + chatroomId);
                        }
    
                        performConnection(chatroomId);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        // If resolving fails but we have TikTok, try to proceed with just TikTok?
                        // For now, let's fail gracefully but check if TikTok was intended
                        if (!tiktokInput.isEmpty()) {
                             // Proceed with empty Kick ID to trigger TikTok-only flow if implemented, 
                             // but performConnection expects valid Kick ID if it tries to connect.
                             // Let's modify performConnection to handle empty Kick ID.
                             performConnection(null);
                        } else {
                            connectBtn.setEnabled(true);
                            statusLabel.setText("Error resolving ID");
                            statusLabel.setForeground(Color.RED);
                        }
                    }
                }
            }.execute();
        } else {
            // No Kick channel, connect directly (TikTok only)
            performConnection(null);
        }
    }

    private void performConnection(String channelId) {
        try {
            if (!overlay.isVisible()) {
                overlay.setVisible(true);
            }
            
            // Update overlay settings just in case
            overlay.setWindowTransparency(opacitySlider.getValue());
            
            // Connect to Kick (only if channelId is provided)
            if (channelId != null && !channelId.isEmpty()) {
                wsClient = new KickWebSocketClient(channelId, overlay, soundCheck, authTokenField.getText().trim());
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
            if (channelId != null && !channelId.isEmpty()) status.append("Kick ");
            if (!tiktokField.getText().trim().isEmpty()) status.append("TikTok");
            
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
        try {
            URL url = new URL("https://kick.com/api/v2/channels/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                if (json.has("chatroom") && json.get("chatroom").isJsonObject()) {
                    return json.get("chatroom").getAsJsonObject().get("id").getAsString();
                }
            } else {
                System.out.println("API Request failed: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.out.println("Error fetching chatroom ID: " + e.getMessage());
        }
        return null;
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