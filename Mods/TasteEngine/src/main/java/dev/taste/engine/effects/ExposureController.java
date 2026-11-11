package dev.taste.engine.effects;

public final class ExposureController {
    private float exposureEv;
    private boolean initialized;
    public float update(float meanLogLuminance, float minimumEv, float maximumEv, float compensation,
            float lightSeconds, float darkSeconds, float deltaSeconds, boolean automatic) {
        float target = automatic ? clamp(log2(0.18F) - meanLogLuminance, minimumEv, maximumEv) + compensation : compensation;
        if (!initialized) { exposureEv = target; initialized = true; return exposureEv; }
        float seconds = target < exposureEv ? Math.max(0.01F, lightSeconds) : Math.max(0.01F, darkSeconds);
        float alpha = 1.0F - (float) Math.exp(-Math.max(0.0F, deltaSeconds) / seconds);
        exposureEv += (target - exposureEv) * alpha;
        return exposureEv;
    }
    public void reset() { initialized = false; }
    private static float log2(float value) { return (float) (Math.log(Math.max(value, 1.0e-5F)) / Math.log(2.0)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
