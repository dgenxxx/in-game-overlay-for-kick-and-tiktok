import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class PlatformIcons {
    private static ImageIcon kickIcon;
    private static ImageIcon tiktokIcon;
    
    public static ImageIcon getKickIcon() {
        if (kickIcon == null) {
            kickIcon = loadIcon("resources/kick.png");
            if (kickIcon == null) {
                kickIcon = createKickIcon();
            }
        }
        return kickIcon;
    }
    
    public static ImageIcon getTikTokIcon() {
        if (tiktokIcon == null) {
            tiktokIcon = loadIcon("resources/tiktok1.jpg");
            if (tiktokIcon == null) {
                tiktokIcon = createTikTokIcon();
            }
        }
        return tiktokIcon;
    }
    
    private static ImageIcon loadIcon(String path) {
        try {
            File file = new File(path);
            System.out.println("PlatformIcons: Attempting to load icon from: " + path);
            System.out.println("PlatformIcons: File exists: " + file.exists());
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(path);
                System.out.println("PlatformIcons: Icon loaded, width=" + icon.getIconWidth() + ", height=" + icon.getIconHeight());
                // Scale to 16x16
                Image scaledImage = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                System.out.println("PlatformIcons: Icon scaled successfully");
                return scaledIcon;
            } else {
                System.out.println("PlatformIcons: File not found, will use programmatic icon");
            }
        } catch (Exception e) {
            System.err.println("PlatformIcons: Error loading icon: " + path);
            e.printStackTrace();
        }
        return null;
    }
    
    private static ImageIcon createKickIcon() {
        // Create 24x24 Kick logo - K shape with left bar and diagonal right sections
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        
        // Bright green (#00FF00)
        g.setColor(new Color(0, 255, 0));
        
        // Left vertical bar
        g.fillRect(2, 0, 6, 24);
        
        // Top right diagonal section (going down-right)
        g.fillRect(8, 0, 16, 3);    // Top bar
        g.fillRect(11, 3, 13, 3);   // Step 1
        g.fillRect(14, 6, 10, 3);   // Step 2
        g.fillRect(17, 9, 7, 2);    // Step 3
        
        // Bottom right diagonal section (going up-right)
        g.fillRect(17, 13, 7, 2);   // Step 3
        g.fillRect(14, 15, 10, 3);  // Step 2
        g.fillRect(11, 18, 13, 3);  // Step 1
        g.fillRect(8, 21, 16, 3);   // Bottom bar
        
        // Middle notch in left bar
        g.setColor(Color.BLACK);
        g.fillRect(3, 10, 4, 4);
        
        g.dispose();
        return new ImageIcon(img);
    }
    
    private static ImageIcon createTikTokIcon() {
        // Create 16x16 TikTok logo (cyan/pink note)
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Musical note shape with TikTok colors
        // Pink shadow
        g.setColor(new Color(255, 0, 80));
        g.fillOval(2, 9, 6, 6);
        g.fillRect(7, 3, 2, 9);
        
        // Cyan main
        g.setColor(new Color(0, 255, 255));
        g.fillOval(1, 8, 6, 6);
        g.fillRect(6, 2, 2, 9);
        
        // White highlight
        g.setColor(Color.WHITE);
        g.fillOval(0, 7, 6, 6);
        g.fillRect(5, 1, 2, 9);
        g.fillOval(5, 1, 4, 4);
        
        g.dispose();
        return new ImageIcon(img);
    }
}
