package dev.taste.engine.environment;

import java.util.ArrayList;
import java.util.List;

/** Deterministic world cells prevent precipitation from following the camera. */
public final class PrecipitationRenderer {
    public enum Type { RAIN, SNOW, HAIL }
    public record Drop(double x, double y, double z, float velocityX, float velocityY, float velocityZ, Type type) { }
    public List<Drop> cells(double cameraX, double cameraY, double cameraZ, int radius, int budget,
            long gameTime, float density, WindField.Wind wind, Type type) {
        List<Drop> result = new ArrayList<>();
        int baseX = (int) Math.floor(cameraX), baseZ = (int) Math.floor(cameraZ);
        for (int z = -radius; z <= radius && result.size() < budget; z++) for (int x = -radius; x <= radius && result.size() < budget; x++) {
            long hash = mix(baseX + x, baseZ + z, gameTime / 4);
            if (((hash >>> 16) & 0xFFFF) / 65535.0F > density) continue;
            double px = baseX + x + ((hash & 255) / 255.0), pz = baseZ + z + (((hash >>> 8) & 255) / 255.0);
            double py = cameraY + radius - ((gameTime * (type == Type.SNOW ? 0.08 : 0.35) + (hash >>> 32 & 255)) % (radius * 2.0));
            float fall = type == Type.SNOW ? -0.08F : type == Type.HAIL ? -0.55F : -0.38F;
            result.add(new Drop(px, py, pz, wind.x(), fall + wind.y(), wind.z(), type));
        }
        return result;
    }
    private static long mix(long x, long z, long t) { long v = x * 73428767L ^ z * 912931L ^ t * 19349663L; v ^= v >>> 33; v *= 0xff51afd7ed558ccdL; return v ^ v >>> 33; }
}
