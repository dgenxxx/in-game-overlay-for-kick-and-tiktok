import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private Properties props = new Properties();
    
    public ConfigManager() {
        load();
    }
    
    public void load() {
        File f = new File(CONFIG_FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void save() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Kick Chat Overlay Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public void setInt(String key, int value) {
        props.setProperty(key, String.valueOf(value));
    }
}