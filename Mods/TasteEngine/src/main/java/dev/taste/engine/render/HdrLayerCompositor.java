package dev.taste.engine.render;

/** CPU reference for the premultiplied layer equation used by the GPU compositor. */
public final class HdrLayerCompositor {
    public float[] over(float[] sourcePremultiplied, float[] destinationPremultiplied) {
        if (sourcePremultiplied.length != 4 || destinationPremultiplied.length != 4)
            throw new IllegalArgumentException("RGBA values required");
        float inverse = 1.0F - sourcePremultiplied[3];
        return new float[] {
                sourcePremultiplied[0] + destinationPremultiplied[0] * inverse,
                sourcePremultiplied[1] + destinationPremultiplied[1] * inverse,
                sourcePremultiplied[2] + destinationPremultiplied[2] * inverse,
                sourcePremultiplied[3] + destinationPremultiplied[3] * inverse
        };
    }
}
