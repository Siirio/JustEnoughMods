package dev.taste.engine.effects.water;

import java.util.List;

public final class SurfaceWaveField {
    public record Wave(float directionX, float directionZ, float amplitude, float wavelength, float speed, float phase) { }
    public record Sample(float height, float normalX, float normalY, float normalZ) { }
    private List<Wave> waves = List.of();
    public void configure(List<Wave> next) { if (next.size() > 8) throw new IllegalArgumentException("Maximum eight waves"); waves = List.copyOf(next); }
    public Sample sample(double x, double z, double time) {
        double height = 0, dx = 0, dz = 0;
        for (Wave wave : waves) {
            double length = Math.hypot(wave.directionX(), wave.directionZ());
            if (length < 1.0e-6 || wave.wavelength() <= 0) continue;
            double dirX = wave.directionX() / length, dirZ = wave.directionZ() / length;
            double k = Math.PI * 2.0 / wave.wavelength();
            double phase = k * (dirX * x + dirZ * z) + wave.phase() + time * wave.speed();
            height += wave.amplitude() * Math.sin(phase);
            double slope = wave.amplitude() * k * Math.cos(phase);
            dx += slope * dirX; dz += slope * dirZ;
        }
        double normalLength = Math.sqrt(dx * dx + 1.0 + dz * dz);
        return new Sample((float) height, (float) (-dx / normalLength), (float) (1.0 / normalLength), (float) (-dz / normalLength));
    }
}
