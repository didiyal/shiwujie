package javax.sound.sampled;

/**
 * 兼容类：为讯飞SDK提供缺失的AudioFormat类
 * 这是一个简化的实现，仅用于满足编译依赖
 */
public class AudioFormat {
    private float sampleRate;
    private int sampleSizeInBits;
    private int channels;
    private boolean signed;
    private boolean bigEndian;
    
    public AudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }
    
    public float getSampleRate() {
        return sampleRate;
    }
    
    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }
    
    public int getChannels() {
        return channels;
    }
    
    public boolean isSigned() {
        return signed;
    }
    
    public boolean isBigEndian() {
        return bigEndian;
    }
    
    // 添加一些常用的静态常量
    public static class Encoding {
        public static final String PCM_SIGNED = "PCM_SIGNED";
        public static final String PCM_UNSIGNED = "PCM_UNSIGNED";
        public static final String PCM_FLOAT = "PCM_FLOAT";
    }
}

