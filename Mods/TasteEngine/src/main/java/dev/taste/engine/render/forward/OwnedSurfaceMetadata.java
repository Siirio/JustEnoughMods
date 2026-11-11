package dev.taste.engine.render.forward;

/** Surface metadata is valid only while its depth still matches final world depth. */
public record OwnedSurfaceMetadata(int normalTexture, int motionTexture, int materialTexture,
        int ownedDepthTexture, long generation) {
    public boolean validFor(long activeGeneration) { return generation == activeGeneration; }
}
