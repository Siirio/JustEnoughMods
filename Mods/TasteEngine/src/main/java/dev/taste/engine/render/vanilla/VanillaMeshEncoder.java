package dev.taste.engine.render.vanilla;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Taste-owned extended terrain vertex. It never mutates DefaultVertexFormat.BLOCK. */
public final class VanillaMeshEncoder {
    public static final int STRIDE = 52;
    public record Vertex(float x, float y, float z, int color, float u, float v, int light, int overlay,
            float normalX, float normalY, float normalZ, int materialId, int generation) { }
    public ByteBuffer encode(Vertex vertex) {
        ByteBuffer out = ByteBuffer.allocateDirect(STRIDE).order(ByteOrder.nativeOrder());
        out.putFloat(vertex.x()).putFloat(vertex.y()).putFloat(vertex.z());
        out.putInt(vertex.color()).putFloat(vertex.u()).putFloat(vertex.v());
        out.putInt(vertex.light()).putInt(vertex.overlay());
        out.putFloat(vertex.normalX()).putFloat(vertex.normalY()).putFloat(vertex.normalZ());
        out.putShort((short) vertex.materialId()).putShort((short) vertex.generation());
        out.flip();
        return out;
    }
}
