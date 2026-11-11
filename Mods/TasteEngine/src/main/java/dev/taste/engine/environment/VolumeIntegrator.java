package dev.taste.engine.environment;

public final class VolumeIntegrator {
    public record Result(float transmittance, float red, float green, float blue) { }
    public Result integrate(float extinction, float distance, float sourceRed, float sourceGreen, float sourceBlue) {
        float sigma = Math.max(0.0F, extinction), length = Math.max(0.0F, distance);
        float transmittance = (float) Math.exp(-sigma * length);
        float integral = sigma < 1.0e-6F ? length : (1.0F - transmittance) / sigma;
        return new Result(transmittance, sourceRed * integral, sourceGreen * integral, sourceBlue * integral);
    }
}
