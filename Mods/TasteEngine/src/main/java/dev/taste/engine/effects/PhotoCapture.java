package dev.taste.engine.effects;

public final class PhotoCapture {
    private float[] accumulation;
    private int frames;
    public void begin(int pixelCount) { accumulation = new float[pixelCount * 4]; frames = 0; }
    public void accumulate(float[] rgba) {
        if (accumulation == null || rgba.length != accumulation.length) throw new IllegalStateException("Photo accumulation not initialized");
        for (int i=0;i<rgba.length;i++) accumulation[i] += rgba[i]; frames++;
    }
    public float[] finish() { if (frames < 1) throw new IllegalStateException("No photo frames"); float[] out=accumulation.clone(); for(int i=0;i<out.length;i++) out[i]/=frames; accumulation=null; frames=0; return out; }
}
