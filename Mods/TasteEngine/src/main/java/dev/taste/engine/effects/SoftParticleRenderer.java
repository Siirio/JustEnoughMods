package dev.taste.engine.effects;

public final class SoftParticleRenderer {
    public float alpha(float particleDepth, float sceneDepth, float softness) {
        return Math.max(0.0F, Math.min(1.0F, (sceneDepth - particleDepth) / Math.max(softness, 1.0e-5F)));
    }
}
