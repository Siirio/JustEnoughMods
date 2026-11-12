package dev.taste.engine.light;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.taste.engine.api.LightProvider.Light;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

/** Injects independent direct radiance and constructs six directional opacity-aware mip chains. */
public final class VoxelRadiancePass implements AutoCloseable {
    private static final int LIGHT_LIMIT = 12;
    private static final int FORMAT = GL30.GL_RGBA16F;

    public static final class Volume implements AutoCloseable {
        private final int resolution;
        private final int levels;
        private int direct;
        private int second;

        private Volume(int resolution, boolean secondBounce) {
            this.resolution = resolution;
            this.levels = Integer.numberOfTrailingZeros(resolution) + 1;
            int atlasSize = resolution * 2;
            try {
                direct = VoxelOccupancyPass.texture(atlasSize, atlasSize, atlasSize, FORMAT,
                        secondBounce ? 1 : levels, true);
                if (secondBounce) {
                    second = VoxelOccupancyPass.texture(atlasSize, atlasSize, atlasSize, FORMAT, levels, true);
                }
            } catch (RuntimeException failure) {
                close();
                throw failure;
            }
        }

        public int texture() { return second == 0 ? direct : second; }
        public int levels() { return levels; }
        public long bytes() { return storageBytes(resolution, second != 0); }

        @Override public void close() {
            if (direct != 0) GL11.glDeleteTextures(direct);
            if (second != 0) GL11.glDeleteTextures(second);
            direct = second = 0;
        }
    }

    private record Uniforms(int occupancy, int orientation, int emission, int source, int minimum,
            int resolution, int operation, int sourceMip, int voxelSize, int sunDirection, int sunRadiance,
            int localCount, int localPosition, int localRadiance, int colorBleed, int emissiveBounce) {
        static Uniforms read(int program) {
            return new Uniforms(location(program, "Occupancy"), location(program, "Orientation"),
                    location(program, "Emission"), location(program, "Source"), location(program, "ClipMinimum"),
                    location(program, "Resolution"), location(program, "Operation"), location(program, "SourceMip"),
                    location(program, "VoxelSize"), location(program, "TowardsSun"), location(program, "SunRadiance"),
                    location(program, "LocalCount"), location(program, "LocalPosition[0]"),
                    location(program, "LocalRadiance[0]"), location(program, "ColorBleed"), location(program, "EmissiveBounce"));
        }
    }

    private int program;
    private Uniforms uniforms;

    public void prepare(ResourceManager resources) throws IOException {
        RenderSystem.assertOnRenderThread();
        if (program != 0) return;
        int next = VoxelOccupancyPass.computeProgram(resources, "taste_voxel_radiance");
        try {
            uniforms = Uniforms.read(next);
            program = next;
        } catch (RuntimeException failure) {
            GL20.glDeleteProgram(next);
            throw failure;
        }
    }

    public Volume createVolume(int resolution, boolean secondBounce) {
        RenderSystem.assertOnRenderThread();
        if (Integer.bitCount(resolution) != 1 || resolution < 8
                || resolution > GL11.glGetInteger(GL12.GL_MAX_3D_TEXTURE_SIZE) / 2)
            throw new IllegalArgumentException("Invalid directional voxel size");
        return new Volume(resolution, secondBounce);
    }

    public static long storageBytes(int resolution, boolean secondBounce) {
        long cells = secondBounce ? cube(resolution * 2) : 0L;
        for (int side = resolution * 2; side >= 2; side /= 2) cells += cube(side);
        return cells * 8L;
    }

    public void update(VoxelOccupancyPass.Volume occupancy, Volume output, VoxelOccupancyPass.Cells clip,
            float voxelSize, Vector3f sunDirection, Vector3f sunRadiance, List<Light> lights,
            float colorBleed, boolean emissiveBounce) {
        RenderSystem.assertOnRenderThread();
        int resolution = occupancy.resolution();
        if (program == 0 || resolution != output.resolution || clip.width() != resolution
                || clip.height() != resolution || clip.depth() != resolution)
            throw new IllegalArgumentException("Directional radiance does not match occupancy bounds");
        try (TextureScope ignored = new TextureScope(4)) {
            GL20.glUseProgram(program);
            bind(0, occupancy.occupancy());
            bind(1, occupancy.orientation());
            bind(2, occupancy.emission());
            GL20.glUniform1i(uniforms.occupancy(), 0);
            GL20.glUniform1i(uniforms.orientation(), 1);
            GL20.glUniform1i(uniforms.emission(), 2);
            GL20.glUniform1i(uniforms.source(), 3);
            GL20.glUniform3i(uniforms.minimum(), clip.x0(), clip.y0(), clip.z0());
            GL20.glUniform1i(uniforms.resolution(), resolution);
            GL20.glUniform1f(uniforms.voxelSize(), voxelSize);
            GL20.glUniform3f(uniforms.sunDirection(), sunDirection.x, sunDirection.y, sunDirection.z);
            GL20.glUniform3f(uniforms.sunRadiance(), sunRadiance.x, sunRadiance.y, sunRadiance.z);
            GL20.glUniform1f(uniforms.colorBleed(), colorBleed);
            GL20.glUniform1i(uniforms.emissiveBounce(), emissiveBounce ? 1 : 0);
            bindLights(clip, voxelSize, lights);
            GL20.glUniform1i(uniforms.operation(), 0);
            bind(3, 0);
            dispatch(output.direct, 0, resolution);
            if (output.second != 0) {
                GL20.glUniform1i(uniforms.operation(), 1);
                bind(3, output.direct);
                dispatch(output.second, 0, resolution);
            }
            bind(3, output.texture());
            GL20.glUniform1i(uniforms.operation(), 2);
            for (int mip = 1; mip < output.levels; mip++) {
                GL20.glUniform1i(uniforms.sourceMip(), mip - 1);
                dispatch(output.texture(), mip, resolution >> mip);
            }
        }
    }

    private void bindLights(VoxelOccupancyPass.Cells clip, float voxelSize, List<Light> lights) {
        int count = Math.min(LIGHT_LIMIT, lights.size());
        GL20.glUniform1i(uniforms.localCount(), count);
        if (count == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer positions = stack.mallocFloat(count * 4);
            FloatBuffer radiance = stack.mallocFloat(count * 4);
            for (int index = 0; index < count; index++) {
                Light light = lights.get(index);
                positions.put((float) (light.x() / voxelSize - clip.x0()));
                positions.put((float) (light.y() / voxelSize - clip.y0()));
                positions.put((float) (light.z() / voxelSize - clip.z0()));
                positions.put(light.range());
                radiance.put(light.red()).put(light.green()).put(light.blue()).put(light.intensity());
            }
            GL20.glUniform4fv(uniforms.localPosition(), positions.flip());
            GL20.glUniform4fv(uniforms.localRadiance(), radiance.flip());
        }
    }

    private static void dispatch(int destination, int level, int size) {
        GL42.glBindImageTexture(0, destination, level, true, 0, GL15.GL_WRITE_ONLY, FORMAT);
        int groups = (size + 3) / 4;
        GL43.glDispatchCompute(groups, groups, groups);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL42.GL_TEXTURE_FETCH_BARRIER_BIT);
    }

    static void bind(int unit, int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, texture);
    }

    private static int location(int program, String uniform) { return VoxelOccupancyPass.uniform(program, uniform); }
    private static long cube(int side) { return (long) side * side * side; }

    static final class TextureScope implements AutoCloseable {
        private final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final VoxelOccupancyPass.ImageBinding image = VoxelOccupancyPass.ImageBinding.read(0);
        private final int[] textures;

        TextureScope(int units) {
            textures = new int[units];
            for (int unit = 0; unit < units; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                textures[unit] = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
            }
        }

        @Override public void close() {
            image.restore(0);
            for (int unit = 0; unit < textures.length; unit++) bind(unit, textures[unit]);
            GL13.glActiveTexture(active);
            GL20.glUseProgram(program);
        }
    }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        if (program != 0) GL20.glDeleteProgram(program);
        program = 0;
        uniforms = null;
    }
}
