package dev.taste.engine.render;

/** CPU half of water_waves.glsl. Parameters are the EXACT float values uploaded to the shader. */
public final class WorldWavePhase {
    private WorldWavePhase() {}
    public static float atOrigin(double originX, double originZ, float directionX, float directionZ,
            float waveNumber, double angularFrequency, double timeSeconds, double seedPhase) {
        double phase = (double) waveNumber * ((double) directionX * originX + (double) directionZ * originZ)
                - angularFrequency * timeSeconds + seedPhase;
        if (!Double.isFinite(phase)) throw new IllegalArgumentException("Non-finite wave phase");
        return (float) Math.IEEEremainder(phase, 2.0 * Math.PI);
    }
}
