package dev.taste.engine.render.embeddium;

/** Packs Taste metadata beside the exact Embeddium 0.3.31 vertex payload. */
public final class EmbeddiumMeshEncoder {
    public long metadata(int materialId, int generation, int flags) {
        if ((materialId & ~0xFFFF) != 0 || (generation & ~0xFFFF) != 0 || (flags & ~0xFFFF) != 0)
            throw new IllegalArgumentException("Packed metadata field overflow");
        return (materialId & 0xFFFFL) | ((generation & 0xFFFFL) << 16) | ((flags & 0xFFFFL) << 32);
    }
}
