package dev.taste.engine.render;

public final class TemporalResolve {
    public float historyWeight(float configured, float reactive, float depthDelta, float disocclusionThreshold, boolean historyValid) {
        if (!historyValid || depthDelta > disocclusionThreshold) return 0.0F;
        return Math.max(0.0F, Math.min(0.99F, configured * (1.0F - reactive)));
    }
    public float[] clip(float[] history, float[] minimum, float[] maximum, float varianceGamma) {
        float[] result = new float[history.length];
        for (int i = 0; i < history.length; i++) {
            float center = (minimum[i] + maximum[i]) * 0.5F;
            float half = (maximum[i] - minimum[i]) * 0.5F * varianceGamma;
            result[i] = Math.max(center - half, Math.min(center + half, history[i]));
        }
        return result;
    }
}
