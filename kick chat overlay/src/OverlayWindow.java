import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

class OverlayWindow extends JFrame {
    private JPanel messagePanel;
    private List<ChatMessage> messages;
    private float opacity = 0.85f;
    private float backgroundAlpha = 0.9f;
    private SoundPlayer soundPlayer;
    private Map<String, Color> roleColors;
    private boolean animationsEnabled = true;
    private ScheduledExecutorService animationExecutor;
    private Point initialClick;
    
    private List<FadableMessagePanel> activeMessagePanels = new CopyOnWriteArrayList<>();
    private int fadeTimeSeconds = 30;
    private javax.swing.Timer fadeTimer;
    private ConfigManager config;
    private boolean locked = false;
    
    public OverlayWindow(boolean soundEnabled, ConfigManager config) {
        this.config = config;
        setTitle("KickChatOverlay_Window");
        System.out.println("OverlayWindow: Constructor called.");
        messages = new ArrayList<ChatMessage>();
        soundPlayer = new SoundPlayer(soundEnabled);
        animationExecutor = Executors.newSingleThreadScheduledExecutor();
        
        fadeTimer = new javax.swing.Timer(50, e -> updateFades());
        fadeTimer.start();
        
        // Initialize role colors
        roleColors = new HashMap<>();
        roleColors.put("broadcaster", new Color(255, 0, 0)); // Red
        roleColors.put("moderator", new Color(0, 255, 0)); // Green
        roleColors.put("vip", new Color(255, 215, 0)); // Gold
        roleColors.put("subscriber", new Color(100, 149, 237)); // Cornflower blue
        roleColors.put("default", new Color(200, 200, 200)); // Light gray
        
        setAlwaysOnTop(true);
        setUndecorated(true); 
        setBackground(new Color(0, 0, 0, 0));
        
        // Load saved bounds
        int x = config.getInt("windowX", -1);
        int y = config.getInt("windowY", -1);
        int w = config.getInt("windowW", 450);
        int h = config.getInt("windowH", 600);
        
        if (x != -1 && y != -1) {
            setLocation(x, y);
        } else {
            // Default size if not saved, position set later
        }
        setSize(w, h);
        
        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                saveBounds();
            }
            public void componentResized(ComponentEvent e) {
                saveBounds();
            }
        });
        
        // Use custom panel for drawing the stylized window
        ChatOverlayPanel contentPane = new ChatOverlayPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setOpaque(false);
        // Add padding for the custom border/header
        contentPane.setBorder(BorderFactory.createEmptyBorder(50, 15, 25, 15));
        setContentPane(contentPane);
        
        messagePanel = new ScrollablePanel(new GridBagLayout());
        messagePanel.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Custom Scrollbar UI
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0, 255, 0, 100);
                this.trackColor = new Color(0, 0, 0, 50);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });
        
        add(scrollPane, BorderLayout.CENTER);
        System.out.println("OverlayWindow: Scroll pane added.");
        
        // Only set default position if not restored from config
        if (x == -1) {
            setPosition("Bottom Left");
            System.out.println("OverlayWindow: Initial position set.");
        } else {
            System.out.println("OverlayWindow: Restored position from config.");
        }

        // Add mouse listeners for dragging the window
        JPanel glassPane = (JPanel) getGlassPane();
        glassPane.setVisible(true);
        glassPane.setOpaque(false); // Make it transparent but active
        
        MouseAdapter dragResizeListener = new MouseAdapter() {
            private boolean isResizing = false;
            private boolean isScrolling = false;
            private int resizeMode = 0; // 0: none, 1: width, 2: height, 3: both
            private int resizeMargin = 10;
            
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                int w = getWidth();
                int h = getHeight();
                
                // Check for scrollbar interaction first
                JScrollBar vScroll = scrollPane.getVerticalScrollBar();
                if (vScroll.isVisible()) {
                    Point pt = SwingUtilities.convertPoint(glassPane, e.getPoint(), vScroll);
                    if (vScroll.contains(pt)) {
                        isScrolling = true;
                        MouseEvent converted = SwingUtilities.convertMouseEvent(glassPane, e, vScroll);
                        vScroll.dispatchEvent(converted);
                        return;
                    }
                }
                
                boolean right = e.getX() >= w - resizeMargin;
                boolean bottom = e.getY() >= h - resizeMargin;
                
                isResizing = false;
                resizeMode = 0;
                
                if (right && bottom) resizeMode = 3;
                else if (bottom) resizeMode = 2;
                else if (right) resizeMode = 1;
                
                if (resizeMode > 0) {
                    isResizing = true;
                } else {
                    getComponentAt(initialClick);
                }
            }
            
            public void mouseDragged(MouseEvent e) {
                if (isScrolling) {
                    JScrollBar vScroll = scrollPane.getVerticalScrollBar();
                    MouseEvent converted = SwingUtilities.convertMouseEvent(glassPane, e, vScroll);
                    vScroll.dispatchEvent(converted);
                    return;
                }
                
                if (isResizing) {
                    int newW = getWidth();
                    int newH = getHeight();
                    
                    if (resizeMode == 1 || resizeMode == 3) newW = e.getX();
                    if (resizeMode == 2 || resizeMode == 3) newH = e.getY();
                    
                    newW = Math.max(newW, 200);
                    newH = Math.max(newH, 200);
                    setSize(newW, newH);
                    revalidate();
                    repaint();
                } else {
                    int thisX = getLocation().x;
                    int thisY = getLocation().y;
                    
                    int xMoved = (thisX + e.getX()) - (thisX + initialClick.x);
                    int yMoved = (thisY + e.getY()) - (thisY + initialClick.y);
                    
                    int X = thisX + xMoved;
                    int Y = thisY + yMoved;
                    setLocation(X, Y);
                }
            }
            
            public void mouseReleased(MouseEvent e) {
                if (isScrolling) {
                    isScrolling = false;
                    JScrollBar vScroll = scrollPane.getVerticalScrollBar();
                    MouseEvent converted = SwingUtilities.convertMouseEvent(glassPane, e, vScroll);
                    vScroll.dispatchEvent(converted);
                }
            }
            
            public void mouseMoved(MouseEvent e) {
                int w = getWidth();
                int h = getHeight();
                boolean right = e.getX() >= w - resizeMargin;
                boolean bottom = e.getY() >= h - resizeMargin;
                
                if (right && bottom) {
                    glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                } else if (bottom) {
                    glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                } else if (right) {
                    glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                } else {
                    glassPane.setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        
        glassPane.addMouseListener(dragResizeListener);
        glassPane.addMouseMotionListener(dragResizeListener);
        
        // Forward scroll events
        glassPane.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                int amount = e.getWheelRotation() * vertical.getUnitIncrement() * 10; 
                vertical.setValue(vertical.getValue() + amount);
            }
        });
    }
    
    public void setRoleColor(String role, Color color) {
        roleColors.put(role.toLowerCase(), color);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        // Ensure title is set correctly for the script to find
        setTitle("KickChatOverlay_Window");
        setFocusableWindowState(!locked);
        // Hide glass pane to disable drag/resize listeners
        getGlassPane().setVisible(!locked);
        
        // Toggle click-through mode using PowerShell script
        toggleClickThrough(locked);
    }
    
    private void toggleClickThrough(final boolean enable) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    System.out.println("OverlayWindow: Toggling click-through to " + enable);
                    String action = enable ? "lock" : "unlock";
                    String scriptPath = new java.io.File("toggle_clickthrough.ps1").getAbsolutePath();
                    
                    ProcessBuilder pb = new ProcessBuilder("powershell.exe", 
                        "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptPath, action);
                    
                    // Capture output for debugging
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("PowerShell Output: " + line);
                    }
                    
                    p.waitFor();
                    System.out.println("OverlayWindow: Click-through toggle finished with exit code " + p.exitValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    public void addTestMessage() {
        ChatMessage msg = new ChatMessage("TestUser", "This is a test message to verify the overlay display.", 
            Color.CYAN, new HashMap<>(), false, false, false, false);
        addMessage(msg);
    }
    
    public void addMessage(final ChatMessage msg) {
        System.out.println("OverlayWindow: Adding message: " + msg.message);
        messages.add(msg);
        if (messages.size() > 50) {
            messages.remove(0);
        }
        
        final List<ChatMessage> messagesCopy = new ArrayList<>(messages); // Create a copy for iteration
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messagePanel.removeAll();
                
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.insets = new Insets(0, 0, 5, 0); // Bottom padding
                
                for (ChatMessage m : messagesCopy) { // Iterate over the copy
                    messagePanel.add(createMessageComponent(m), gbc);
                    gbc.gridy++;
                }
                
                // Add filler to push messages up
                GridBagConstraints fillerGbc = new GridBagConstraints();
                fillerGbc.gridx = 0;
                fillerGbc.gridy = gbc.gridy;
                fillerGbc.weighty = 1.0;
                messagePanel.add(Box.createGlue(), fillerGbc);
                
                messagePanel.revalidate();
                messagePanel.repaint();
            }
        });
        
        soundPlayer.playSound();
    }
    
    private JPanel createMessageComponent(ChatMessage msg) {
        FadableMessagePanel panel = new FadableMessagePanel(new BorderLayout());
        panel.setOpaque(false); // Transparent background
        panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10)); // Clean padding, no box
        
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setFocusable(false);
        
        // Add breathing room for text
        javax.swing.text.MutableAttributeSet paragraphStyle = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setLineSpacing(paragraphStyle, 0.2f);
        textPane.setParagraphAttributes(paragraphStyle, false);
        
        javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
        
        // User style
        javax.swing.text.SimpleAttributeSet userStyle = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setForeground(userStyle, msg.color);
        javax.swing.text.StyleConstants.setBold(userStyle, true);
        javax.swing.text.StyleConstants.setFontFamily(userStyle, "Arial");
        javax.swing.text.StyleConstants.setFontSize(userStyle, 16);
        
        // Message style
        javax.swing.text.SimpleAttributeSet msgStyle = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setForeground(msgStyle, Color.WHITE);
        javax.swing.text.StyleConstants.setFontFamily(msgStyle, "Arial");
        javax.swing.text.StyleConstants.setFontSize(msgStyle, 16);
        
        try {
            doc.insertString(doc.getLength(), msg.username + ": ", userStyle);
            doc.insertString(doc.getLength(), msg.message, msgStyle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        panel.add(textPane, BorderLayout.CENTER);
        
        activeMessagePanels.add(panel);
        return panel;
    }
    
    public void setOverlayOpacity(float opacity) {
        // Redirect to new transparency system logic if needed, or keep mostly independent
        // User requested "replace", so let's map it to window transparency?
        // But existing calls use 0.85 etc.
        // Let's keep this for backward compatibility but map it to window transparency if we want global effect
        // OR just ignore it if the slider in ControlPanel now calls setWindowTransparency
        this.opacity = opacity;
        // setWindowTransparency((int)(opacity * 100)); // Uncomment if we want to enforce it here
    }
    
    public void setWindowTransparency(int value) {
        // Controls background opacity only. 100 = Fully Opaque Background, 0 = Fully Transparent Background.
        this.backgroundAlpha = value / 100.0f;
        setOpacity(1.0f); // Ensure window contents remain visible
        repaint();
    }

    public int getWindowTransparency() {
        return (int)(backgroundAlpha * 100);
    }

    public void setFadeTime(int seconds) {
        this.fadeTimeSeconds = seconds;
    }

    private void updateFades() {
        long now = System.currentTimeMillis();
        long fadeDuration = 1000;
        long timeoutMillis = fadeTimeSeconds * 1000L;
        
        boolean changed = false;
        for (FadableMessagePanel panel : activeMessagePanels) {
            long age = now - panel.creationTime;
            if (age > timeoutMillis + fadeDuration) {
                messagePanel.remove(panel);
                activeMessagePanels.remove(panel);
                changed = true;
            } else if (age > timeoutMillis) {
                float progress = (float)(age - timeoutMillis) / fadeDuration;
                panel.setAlpha(1.0f - progress);
            }
        }
        if (changed) {
            messagePanel.revalidate();
            messagePanel.repaint();
        }
    }
    
    private void saveBounds() {
        if (config != null) {
            config.setInt("windowX", getX());
            config.setInt("windowY", getY());
            config.setInt("windowW", getWidth());
            config.setInt("windowH", getHeight());
            config.save();
        }
    }

    public void setPosition(String position) {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        int x = 0;
        int y = 0;
        
        if (position.equals("Top Left")) {
            x = 20;
            y = 20;
        } else if (position.equals("Top Right")) {
            x = screenWidth - getWidth() - 20;
            y = 20;
        } else if (position.equals("Bottom Left")) {
            x = 20;
            y = screenHeight - getHeight() - 60;
        } else if (position.equals("Bottom Right")) {
            x = screenWidth - getWidth() - 20;
            y = screenHeight - getHeight() - 60;
        }
        
        setLocation(x, y);
    }
    
    public void clearMessages() {
        messages.clear();
        messagePanel.removeAll();
        messagePanel.revalidate();
        messagePanel.repaint();
    }
    
    private void refreshMessages() {
        messagePanel.removeAll();
        for (ChatMessage m : messages) {
            messagePanel.add(createMessageComponent(m));
            messagePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        messagePanel.revalidate();
        messagePanel.repaint();
    }

    // Inner class for custom painting
    private class ChatOverlayPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cornerSize = 20;
            int headerHeight = 40;
            int headerWidth = 120;
            
            // Background
            int alpha = (int)(backgroundAlpha * 255);
            alpha = Math.max(0, Math.min(255, alpha));
            g2d.setColor(new Color(20, 20, 20, alpha));
            
            // Main body with angled bottom corners
            int[] xPoints = {0, w, w, w-cornerSize, cornerSize, 0};
            int[] yPoints = {headerHeight/2, headerHeight/2, h-cornerSize, h, h, h-cornerSize};
            g2d.fillPolygon(xPoints, yPoints, 6);
            
            // Header path
            java.awt.geom.GeneralPath headerPath = new java.awt.geom.GeneralPath();
            headerPath.moveTo(0, headerHeight/2);
            headerPath.lineTo((w - headerWidth)/2 - 20, headerHeight/2);
            headerPath.lineTo((w - headerWidth)/2, 0);
            headerPath.lineTo((w + headerWidth)/2, 0);
            headerPath.lineTo((w + headerWidth)/2 + 20, headerHeight/2);
            headerPath.lineTo(w, headerHeight/2);
            
            // Draw Header Background
            g2d.setColor(new Color(30, 30, 30));
            g2d.fill(headerPath);
            
            // Draw Green Border/Glow
            g2d.setColor(Color.GREEN);
            g2d.setStroke(new BasicStroke(2f));
            
            // Top Border (including header)
            g2d.draw(headerPath);
            
            // Side and Bottom Borders
            g2d.drawLine(0, headerHeight/2, 0, h-cornerSize); // Left
            g2d.drawLine(w, headerHeight/2, w, h-cornerSize); // Right
            g2d.drawLine(0, h-cornerSize, cornerSize, h); // Bottom Left Corner
            g2d.drawLine(w, h-cornerSize, w-cornerSize, h); // Bottom Right Corner
            g2d.drawLine(cornerSize, h, w-cornerSize, h); // Bottom Edge
            
            // Draw "CHAT" text
            g2d.setFont(new Font("Impact", Font.BOLD, 24));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "CHAT";
            int textX = (w - fm.stringWidth(text)) / 2;
            int textY = 28;
            
            // Text Glow/Shadow
            g2d.setColor(new Color(0, 255, 0, 100));
            g2d.drawString(text, textX+1, textY+1);
            
            g2d.setColor(Color.GREEN);
            g2d.drawString(text, textX, textY);
            
            g2d.dispose();
        }
    }

    private class ScrollablePanel extends JPanel implements Scrollable {
        public ScrollablePanel(LayoutManager layout) {
            super(layout);
        }
        
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
        
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }
        
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // Force width to match viewport -> Forces wrapping
        }
        
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private class FadableMessagePanel extends JPanel {
        long creationTime = System.currentTimeMillis();
        float alpha = 1.0f;
        
        public FadableMessagePanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }
        
        public void setAlpha(float alpha) {
            this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            repaint();
        }
        
        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paint(g2);
            g2.dispose();
        }
    }
}