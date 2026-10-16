package com.siirio.jemvisualsandbox.r3;

import java.io.DataInputStream;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Native Blockbench OBJ poses: quads and top-down UVs, measured in blocks. */
final class BloomR3Mesh {
    static final ResourceLocation[] TEXTURES = {
        location("bark.png"), location("foliage.png"), location("bud.png")
    };
    final int[] materials;
    final float[][] uv;
    final float[][] poses;
    final float step;

    private BloomR3Mesh(int[] materials, float[][] uv, float[][] poses, float step) {
        this.materials = materials;
        this.uv = uv;
        this.poses = poses;
        this.step = step;
    }

    static ResourceLocation location(String file) {
        return new ResourceLocation("jem_visual_sandbox", "r3/" + file);
    }

    static BloomR3Mesh load() throws IOException {
        var resource = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(location("verdant_grasp.mesh"));
        try (var input = new DataInputStream(resource.open())) {
            if (input.readInt() != 0x4a523301) throw new IOException("Invalid R3 mesh header");
            int frameCount = input.readInt();
            int faceCount = input.readInt();
            float step = input.readFloat();
            if (frameCount != 45 || faceCount != 294 || step != 0.05F)
                throw new IOException("Unexpected R3 mesh dimensions");
            int[] materials = new int[faceCount];
            float[][] uv = new float[faceCount][8];
            for (int face = 0; face < faceCount; face++) {
                materials[face] = input.readUnsignedByte();
                if (materials[face] >= TEXTURES.length) throw new IOException("Invalid R3 material");
                for (int axis = 0; axis < 8; axis++) uv[face][axis] = finite(input);
            }
            float[][] poses = new float[frameCount][faceCount * 12];
            for (float[] frame : poses)
                for (int axis = 0; axis < frame.length; axis++) frame[axis] = finite(input);
            if (input.read() != -1) throw new IOException("Trailing R3 mesh data");
            return new BloomR3Mesh(materials, uv, poses, step);
        }
    }

    private static float finite(DataInputStream input) throws IOException {
        float value = input.readFloat();
        if (!Float.isFinite(value)) throw new IOException("Non-finite R3 mesh coordinate");
        return value;
    }
}
