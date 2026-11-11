package dev.taste.engine.render;

/** Immutable allocation contract. Byte estimates include every mip, layer and sample. */
public record TargetDescriptor(String name, int width, int height, Format format, boolean depth,
        int mipLevels, int layers, int samples) {
    public enum Format {
        RGBA8(4), RGBA16F(8), RG16F(4), R16F(2), R8(1), DEPTH24(4), DEPTH32F(4);
        private final int bytesPerPixel;
        Format(int bytesPerPixel) { this.bytesPerPixel = bytesPerPixel; }
        public int bytesPerPixel() { return bytesPerPixel; }
    }

    public TargetDescriptor {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Target name is required");
        if (width < 1 || height < 1 || mipLevels < 1 || layers < 1 || samples < 1)
            throw new IllegalArgumentException("Invalid target dimensions");
    }

    public long byteSize() {
        long total = 0;
        int mipWidth = width;
        int mipHeight = height;
        for (int level = 0; level < mipLevels; level++) {
            total = Math.addExact(total, Math.multiplyExact((long) mipWidth * mipHeight,
                    (long) format.bytesPerPixel() * layers * samples));
            mipWidth = Math.max(1, mipWidth / 2);
            mipHeight = Math.max(1, mipHeight / 2);
        }
        if (depth && format != Format.DEPTH24 && format != Format.DEPTH32F) {
            total = Math.addExact(total, Math.multiplyExact((long) width * height, 4L * layers * samples));
        }
        return total;
    }
}
