package dev.taste.engine.effects.water;

public final class UnderwaterComposite {
    public float[] apply(float[] color, float distance, float[] absorption, float[] scattering) {
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            float transmittance = (float) Math.exp(-Math.max(0, absorption[i]) * Math.max(0, distance));
            result[i] = color[i] * transmittance + scattering[i] * (1.0F - transmittance);
        }
        return result;
    }
}
