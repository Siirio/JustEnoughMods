package dev.taste.engine.environment;

public final class SkyRenderer {
    public record SkyState(float sunX, float sunY, float sunZ, float sunVisibility,
            float moonVisibility, float starVisibility, float red, float green, float blue) { }
    public SkyState resolve(EnvironmentSnapshot environment, DimensionProfiles.Profile profile) {
        float angle = environment.sunAngle();
        float sunY = (float) Math.cos(angle);
        float sunX = (float) Math.sin(angle);
        float daylight = clamp(sunY * 0.65F + 0.45F);
        float weather = 1.0F - clamp(environment.rain() * 0.7F + environment.thunder() * 0.25F);
        return new SkyState(sunX, sunY, 0.0F, daylight * weather, (1.0F - daylight) * weather,
                (1.0F - daylight) * profile.starStrength() * weather,
                profile.red(), profile.green(), profile.blue());
    }
    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
