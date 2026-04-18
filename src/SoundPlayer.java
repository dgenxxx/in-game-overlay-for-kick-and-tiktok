import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

class SoundPlayer {
    private Clip clip;
    private boolean enabled;
    
    public SoundPlayer(boolean enabled) {
        this.enabled = enabled;
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            byte[] soundData = generateBeep(format);
            
            AudioInputStream audioStream = new AudioInputStream(
                new ByteArrayInputStream(soundData),
                format,
                soundData.length / format.getFrameSize()
            );
            
            clip = AudioSystem.getClip();
            clip.open(audioStream);
        } catch (Exception e) {
            System.err.println("Error initializing sound: " + e.getMessage());
        }
    }
    
    private byte[] generateBeep(AudioFormat format) {
        int duration = (int)(0.1 * format.getSampleRate());
        byte[] data = new byte[duration * 2];
        
        for (int i = 0; i < duration; i++) {
            double angle = 2.0 * Math.PI * i / (format.getSampleRate() / 800);
            short value = (short)(Math.sin(angle) * 3000);
            data[i * 2] = (byte)(value & 0xFF);
            data[i * 2 + 1] = (byte)((value >> 8) & 0xFF);
        }
        return data;
    }
    
    public void playSound() {
        if (enabled && clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
}