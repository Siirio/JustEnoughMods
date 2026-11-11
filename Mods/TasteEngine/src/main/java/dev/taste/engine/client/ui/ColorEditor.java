package dev.taste.engine.client.ui;

public final class ColorEditor {
    public int parse(String text) {
        if (text == null || !text.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Expected #RRGGBB");
        return Integer.parseInt(text.substring(1), 16);
    }
    public String format(int rgb) { return String.format("#%06X", rgb & 0xFFFFFF); }
    public float[] linear(int rgb) {
        return new float[] { decode((rgb>>16)&255), decode((rgb>>8)&255), decode(rgb&255) };
    }
    private static float decode(int channel) { float c=channel/255.0F; return c<=0.04045F?c/12.92F:(float)Math.pow((c+0.055F)/1.055F,2.4); }
}
