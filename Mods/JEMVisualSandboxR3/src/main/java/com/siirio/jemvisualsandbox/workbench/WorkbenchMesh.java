package com.siirio.jemvisualsandbox.workbench;

import java.io.DataInputStream;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Native Blockbench OBJ poses: quads and top-down UVs, measured in blocks. */
final class WorkbenchMesh {
    final ResourceLocation[] textures;
    final int[] materials;
    final float[][] uv;
    final float[][] poses;
    final float step;

    private WorkbenchMesh(int[] materials, float[][] uv, float[][] poses, float step, ResourceLocation[] textures) {
        this.materials = materials;
        this.uv = uv;
        this.poses = poses;
        this.step = step;
        this.textures = textures;
    }

    static ResourceLocation location(String file) {
        return new ResourceLocation("jem_visual_sandbox", "workbench/" + file);
    }

    static WorkbenchMesh load(String id, int textureCount) throws IOException {
        var resource = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(id.equals("bloom") ? new ResourceLocation("jem_visual_sandbox", "r3/verdant_grasp.mesh") : location(id + ".mesh"));
        try (var input = new DataInputStream(resource.open())) {
            if (input.readInt() != 0x4a523301) throw new IOException("Invalid R3 mesh header");
            int frameCount = input.readInt();
            int faceCount = input.readInt();
            float step = input.readFloat();
            if (frameCount < 2 || frameCount > 241 || faceCount < 1 || faceCount > 4096 || step != 0.05F || textureCount < 1 || textureCount > 8)
                throw new IOException("Unexpected R3 mesh dimensions");
            ResourceLocation[] textures = new ResourceLocation[textureCount];
            String[] bloom = {"bark", "foliage", "bud"};
            for (int i = 0; i < textures.length; i++) {
                textures[i] = id.equals("bloom") ? new ResourceLocation("jem_visual_sandbox", "r3/" + bloom[i] + ".png") : location(id + "-" + i + ".png");
                Minecraft.getInstance().getResourceManager().getResourceOrThrow(textures[i]);
            }
            int[] materials = new int[faceCount];
            float[][] uv = new float[faceCount][8];
            for (int face = 0; face < faceCount; face++) {
                materials[face] = input.readUnsignedByte();
                if (materials[face] >= textures.length) throw new IOException("Invalid R3 material");
                for (int axis = 0; axis < 8; axis++) uv[face][axis] = finite(input);
            }
            float[][] poses = new float[frameCount][faceCount * 12];
            for (float[] frame : poses)
                for (int axis = 0; axis < frame.length; axis++) frame[axis] = finite(input);
            if (input.read() != -1) throw new IOException("Trailing R3 mesh data");
            return new WorkbenchMesh(materials, uv, poses, step, textures);
        }
    }

    private static float finite(DataInputStream input) throws IOException {
        float value = input.readFloat();
        if (!Float.isFinite(value)) throw new IOException("Non-finite R3 mesh coordinate");
        return value;
    }
}
