package dev.taste.engine.effects;

public final class ColorPipeline {
    public float[] filmic(float[] color) { float[] out = new float[3]; for (int i=0;i<3;i++){ float x=Math.max(0,color[i]); out[i]=clamp((x*(2.51F*x+0.03F))/(x*(2.43F*x+0.59F)+0.14F)); } return out; }
    public float[] neutral(float[] input) {
        float[] color = new float[] { Math.max(0,input[0]), Math.max(0,input[1]), Math.max(0,input[2]) };
        float x = Math.min(color[0], Math.min(color[1], color[2]));
        float offset = x < 0.08F ? x - 6.25F * x * x : 0.04F;
        for (int i=0;i<3;i++) color[i] -= offset;
        float peak = Math.max(color[0], Math.max(color[1], color[2]));
        if (peak < 0.76F) return color;
        float q = 1.0F - 0.0576F / (peak + 0.24F - 0.76F);
        float desaturate = 1.0F - 1.0F / (0.15F * (peak - q) + 1.0F);
        for (int i=0;i<3;i++) color[i] = mix(color[i] * q / peak, q, desaturate);
        return color;
    }
    public float luminance(float[] color) { return color[0]*0.2126F+color[1]*0.7152F+color[2]*0.0722F; }
    private static float mix(float a,float b,float t){return a+(b-a)*t;}
    private static float clamp(float value){return Math.max(0,Math.min(1,value));}
}
