package dev.taste.engine.light;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.taste.engine.api.LightProvider.Light;
import dev.taste.engine.material.MaterialResolver;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

/** Owns voxel GI generations and a separate indirect-only resolve target. */
public final class VoxelGiPass implements AutoCloseable {
    private static final long DEFAULT_CPU_BUDGET = 48L * 1024L * 1024L;
    private static final int MAX_DIRTY_EVENTS = 256;
    private static final int MAX_CLIPMAPS = 4;
    private static final int SCREEN_TEXTURES = 4;
    private static final String[] CLIP_SAMPLERS = {"Clip0", "Clip1", "Clip2", "Clip3"};

    private record Layout(int count, int resolution, float nearSize, boolean secondBounce) { }
    private record History(FrameSnapshot frame, long generation, long configRevision, TextureTarget texture) { }
    private record Lighting(Vector3f direction, Vector3f radiance, List<Light> local, float colorBleed, boolean emissive) { }

    private final VoxelOccupancyPass occupancy = new VoxelOccupancyPass();
    private final VoxelRadiancePass radiance = new VoxelRadiancePass();
    private final VoxelSectionCache sections;
    private final Set<BlockPos> dirty = new LinkedHashSet<>();
    private List<VoxelClipmapVolume> volumes = List.of();
    private VoxelClipmaps layoutModel;
    private Layout layout;
    private Lighting lastLighting;
    private History previous;
    private int program;
    private int vao;
    private int outputIndex;
    private int nextVolume;
    private long worldEpoch = -1L;
    private long generation;
    private long lastLightingFrame = -1L;
    private boolean resetOccupancy;

    public VoxelGiPass() { this(new VoxelSectionCache(DEFAULT_CPU_BUDGET)); }

    public VoxelGiPass(VoxelSectionCache sections) { this.sections = sections; }

    public void prepare(ResourceManager resources) throws IOException {
        RenderSystem.assertOnRenderThread();
        occupancy.prepare(resources);
        radiance.prepare(resources);
        if (program == 0) {
            program = link(resources);
            vao = GL30.glGenVertexArrays();
        }
    }

    /**
     * Produces RGB indirect irradiance and preserves optional screen AO in alpha.
     * The supplied depth target is the pre-translucent opaque capture.
     */
    public TextureTarget render(TargetPool targets, RenderTarget opaque, TextureTarget ambient, FrameSnapshot frame,
            PipelineParameters parameters, ClientLevel level, MaterialResolver materials, Vector3f towardsLight,
            Vector3f lightColor, float lightIntensity, List<Light> localLights) throws IOException {
        if (opaque == null || level == null) return ambient;
        prepare(Minecraft.getInstance().getResourceManager());
        Layout requested = layout(parameters);
        int width = ambient == null ? Math.max(1, opaque.width / 2) : ambient.width;
        int height = ambient == null ? Math.max(1, opaque.height / 2) : ambient.height;
        ensureLayout(requested, frame, targets, parameters, width, height);
        updateVolumes(frame, parameters, level, materials, towardsLight, lightColor, lightIntensity, localLights);
        return resolve(targets, opaque, ambient, frame, parameters, lightColor, lightIntensity, width, height);
    }

    /** May be called by the world change hook before the next render pass. */
    public void dirty(BlockPos position) {
        sections.dirty(position);
        if (resetOccupancy) return;
        if (dirty.size() >= MAX_DIRTY_EVENTS) {
            dirty.clear();
            resetOccupancy = true;
        } else {
            dirty.add(position.immutable());
        }
    }

    public long allocatedBytes() { return layout == null ? 0L : bytes(layout); }

    public static long requestedBytes(PipelineParameters settings) { return bytes(layout(settings)); }

    public void invalidate() {
        previous = null;
        outputIndex = 0;
    }

    public void invalidateWorld() {
        invalidate();
        sections.clear();
        resetOccupancy = true;
    }

    public void releaseTargets(TargetPool targets) {
        targets.release("voxel-indirect-0");
        targets.release("voxel-indirect-1");
        close();
    }

    private void ensureLayout(Layout requested, FrameSnapshot frame, TargetPool targets, PipelineParameters parameters,
            int width, int height) {
        if (requested.equals(layout)) {
            if (worldEpoch != frame.worldEpoch() || resetOccupancy) {
                volumes.forEach(VoxelClipmapVolume::reset);
                worldEpoch = frame.worldEpoch();
                resetOccupancy = false;
                lastLighting = null;
                dirty.clear();
                invalidate();
            }
            return;
        }
        long limit = parameters.value("performance.memoryBudgetMiB").getAsLong() * 1024L * 1024L;
        long otherGpuBytes = Math.max(0L, targets.externalBytes() - allocatedBytes());
        long temporary = allocatedBytes() + bytes(requested) + targets.allocatedBytes() + otherGpuBytes;
        long resolveBytes = (long) width * height * 24L;
        if (temporary + resolveBytes > limit)
            throw new IllegalStateException("Voxel clipmap allocation exceeds the configured GPU memory budget");
        List<VoxelClipmapVolume> next = new ArrayList<>(requested.count());
        try {
            for (int index = 0; index < requested.count(); index++) {
                next.add(new VoxelClipmapVolume(occupancy, radiance, requested.resolution(),
                        requested.nearSize() * (1 << index), requested.secondBounce()));
            }
        } catch (RuntimeException failure) {
            next.forEach(VoxelClipmapVolume::close);
            throw failure;
        }
        volumes.forEach(VoxelClipmapVolume::close);
        volumes = List.copyOf(next);
        layout = requested;
        layoutModel = new VoxelClipmaps(layout.count(), layout.resolution(), layout.nearSize());
        worldEpoch = frame.worldEpoch();
        resetOccupancy = false;
        generation++;
        dirty.clear();
        nextVolume = 0;
        lastLighting = null;
        invalidate();
    }

    private void updateVolumes(FrameSnapshot frame, PipelineParameters parameters, ClientLevel level,
            MaterialResolver materials, Vector3f direction, Vector3f color, float intensity, List<Light> lights) {
        var eye = frame.cameraWorld();
        VoxelClipmaps.Clipmap[] centered = layoutModel.center(eye.x(), eye.y(), eye.z());
        for (int index = 0; index < volumes.size(); index++) {
            VoxelClipmapVolume volume = volumes.get(index);
            volume.move(centered[index]);
            if (frame.frameIndex() % 16 == 0) volume.refreshChunks(level);
            for (BlockPos position : dirty) volume.dirty(position);
        }
        dirty.clear();
        int atlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
        int slices = parameters.value("gi.updateSlices").getAsInt();
        for (int count = 0; count < volumes.size() && slices > 0; count++) {
            VoxelClipmapVolume volume = volumes.get(nextVolume);
            nextVolume = (nextVolume + 1) % volumes.size();
            if (!volume.pending()) continue;
            int allowance = Math.max(1, slices / (volumes.size() - count));
            slices -= volume.update(allowance, atlas, level, sections, materials);
        }

        Lighting current = new Lighting(new Vector3f(direction), new Vector3f(color).mul(Math.max(0.0F, intensity)),
                List.copyOf(lights), value(parameters, "gi.colorBleed"), parameters.value("gi.emissiveBounce").getAsBoolean());
        boolean relight = lastLighting == null || (!lastLighting.equals(current)
                && frame.frameIndex() - lastLightingFrame >= 4);
        boolean changed = false;
        Lighting injection = relight ? current : lastLighting;
        for (VoxelClipmapVolume volume : volumes) {
            if (!volume.dirtyRadiance() && !relight) continue;
            radiance.update(volume.occupancy(), volume.radiance(), volume.clip(), volume.voxelSize(),
                    injection.direction(), injection.radiance(), injection.local(), injection.colorBleed(), injection.emissive());
            volume.radianceReady();
            changed = true;
        }
        if (changed) generation++;
        if (relight) {
            lastLighting = current;
            lastLightingFrame = frame.frameIndex();
        }
    }

    private TextureTarget resolve(TargetPool targets, RenderTarget opaque, TextureTarget ambient, FrameSnapshot frame,
            PipelineParameters parameters, Vector3f skyColor, float skyIntensity, int width, int height) {
        float weight = value(parameters, "gi.temporalWeight");
        boolean history = acceptsHistory(frame, parameters, width, height, weight);
        if (weight <= 0.0F) {
            targets.release("voxel-indirect-1");
            invalidate();
        }
        TextureTarget output = targets.acquire(new TargetDescriptor("voxel-indirect-" + outputIndex, width, height,
                TargetDescriptor.Format.RGBA16F, true, 1, 1, 1));
        copyDepth(opaque, output);
        output.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(true, true, true, true);

        try (var ignored = new VoxelRadiancePass.TextureScope(SCREEN_TEXTURES + MAX_CLIPMAPS + 1);
                var textures = new ScreenScope(SCREEN_TEXTURES)) {
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vao);
            textures.bind(0, opaque.getDepthTextureId());
            textures.bind(1, ambient == null ? opaque.getDepthTextureId() : ambient.getColorTextureId());
            textures.bind(2, history ? previous.texture().getColorTextureId() : opaque.getDepthTextureId());
            textures.bind(3, history ? previous.texture().getDepthTextureId() : opaque.getDepthTextureId());
            integer("ViewDepth", 0);
            integer("Ambient", 1);
            integer("HistoryRadiance", 2);
            integer("HistoryDepth", 3);
            integer("HasAmbient", ambient == null ? 0 : 1);
            for (int index = 0; index < MAX_CLIPMAPS; index++) {
                VoxelClipmapVolume clip = volumes.get(Math.min(index, volumes.size() - 1));
                int unit = SCREEN_TEXTURES + index;
                VoxelRadiancePass.bind(unit, clip.radiance().texture());
                integer(CLIP_SAMPLERS[index], unit);
                float size = clip.voxelSize();
                var bounds = clip.clip();
                var origin = frame.origin();
                GL20.glUniform4f(uniform("ClipBounds[" + index + "]"),
                        (float) (bounds.x0() * (double) size - origin.x()),
                        (float) (bounds.y0() * (double) size - origin.y()),
                        (float) (bounds.z0() * (double) size - origin.z()), size);
            }
            var near = volumes.get(0);
            int skyUnit = SCREEN_TEXTURES + MAX_CLIPMAPS;
            VoxelRadiancePass.bind(skyUnit, near.occupancy().orientation());
            integer("SurfaceSky", skyUnit);
            GL20.glUniform3i(uniform("NearClipCell"), near.clip().x0(), near.clip().y0(), near.clip().z0());
            GL20.glUniform3f(uniform("SkyRadiance"), skyColor.x * skyIntensity,
                    skyColor.y * skyIntensity, skyColor.z * skyIntensity);
            integer("Resolution", layout.resolution());
            integer("ClipCount", layout.count());
            integer("ConeCount", parameters.value("gi.cones").getAsInt());
            scalar("DistanceLimit", value(parameters, "gi.maxDistance"));
            scalar("OpacityBias", value(parameters, "gi.opacityBias"));
            scalar("Intensity", value(parameters, "gi.intensity"));
            scalar("TemporalWeight", history ? weight : 0.0F);
            matrix("InverseProjection", from(frame.inverseProjection()));
            matrix("InverseView", from(frame.view()).invert());
            if (history) bindHistory(frame);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        }
        previous = new History(frame, generation, parameters.revision(), output);
        if (weight > 0.0F) outputIndex ^= 1;
        return output;
    }

    private void bindHistory(FrameSnapshot frame) {
        FrameSnapshot old = previous.frame();
        float[] relative = frame.origin().relativeTo(old.origin());
        Matrix4f oldView = from(old.view()).translate(relative[0], relative[1], relative[2]);
        matrix("PreviousClip", from(old.projection()).mul(oldView));
        matrix("PreviousInverseProjection", from(old.inverseProjection()));
        matrix("PreviousInverseView", oldView.invert());
    }

    private boolean acceptsHistory(FrameSnapshot frame, PipelineParameters parameters, int width, int height, float weight) {
        if (previous == null || weight <= 0.0F || previous.generation() != generation
                || previous.configRevision() != parameters.revision()) return false;
        var old = previous.frame();
        return previous.texture().width == width && previous.texture().height == height
                && old.worldEpoch() == frame.worldEpoch() && old.frameIndex() + 1 == frame.frameIndex()
                && old.unjitteredProjection().equals(frame.unjitteredProjection());
    }

    private static void copyDepth(RenderTarget source, TextureTarget output) {
        RenderSystem.disableScissor();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, output.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, source.width, source.height, 0, 0, output.width, output.height,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
    }

    private static Layout layout(PipelineParameters parameters) {
        return new Layout(parameters.value("gi.clipmaps").getAsInt(),
                Integer.parseInt(parameters.value("gi.resolution").getAsString()),
                value(parameters, "gi.nearVoxelSize"), parameters.value("gi.secondBounce").getAsBoolean());
    }

    private static long bytes(Layout layout) {
        long count = (long) layout.resolution() * layout.resolution() * layout.resolution();
        long radiance = VoxelRadiancePass.storageBytes(layout.resolution(), layout.secondBounce());
        return VoxelOccupancyPass.scratchBytes(layout.resolution()) + layout.count() * (count * 16L + radiance);
    }

    private int uniform(String name) { return VoxelOccupancyPass.uniform(program, name); }
    private void integer(String name, int value) { GL20.glUniform1i(uniform(name), value); }
    private void scalar(String name, float value) { GL20.glUniform1f(uniform(name), value); }
    private void matrix(String name, Matrix4f matrix) {
        try (var stack = MemoryStack.stackPush()) {
            GL20.glUniformMatrix4fv(uniform(name), false, matrix.get(stack.mallocFloat(16)));
        }
    }

    private static Matrix4f from(FrameSnapshot.Matrix4 matrix) { return new Matrix4f().set(matrix.columnMajor()); }
    private static float value(PipelineParameters parameters, String id) { return parameters.value(id).getAsFloat(); }

    private static int link(ResourceManager resources) throws IOException {
        int vertex = compile(resources, GL20.GL_VERTEX_SHADER, "taste_voxel_cones.vert");
        int fragment = 0;
        int program = 0;
        try {
            fragment = compile(resources, GL20.GL_FRAGMENT_SHADER, "taste_voxel_cones.frag");
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
                throw new IOException("Voxel cone link failed: " + GL20.glGetProgramInfoLog(program));
            GL20.glDetachShader(program, vertex);
            GL20.glDetachShader(program, fragment);
            return program;
        } catch (IOException | RuntimeException failure) {
            if (program != 0) GL20.glDeleteProgram(program);
            throw failure;
        } finally {
            GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
        }
    }

    private static int compile(ResourceManager resources, int type, String name) throws IOException {
        String source;
        try (var input = resources.getResourceOrThrow(new ResourceLocation("taste", "shaders/gi/" + name)).open()) {
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE) return shader;
        String log = GL20.glGetShaderInfoLog(shader);
        GL20.glDeleteShader(shader);
        throw new IOException(name + " compile failed: " + log);
    }

    private static final class ScreenScope implements AutoCloseable {
        private final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        private final int[] previous;

        ScreenScope(int count) {
            previous = new int[count];
            for (int index = 0; index < count; index++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + index);
                previous[index] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
        }

        void bind(int unit, int texture) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }

        @Override public void close() {
            for (int index = 0; index < previous.length; index++) bind(index, previous[index]);
            GL30.glBindVertexArray(vao);
            GL13.glActiveTexture(active);
        }
    }

    @Override public void close() {
        volumes.forEach(VoxelClipmapVolume::close);
        volumes = List.of();
        occupancy.close();
        radiance.close();
        if (program != 0) GL20.glDeleteProgram(program);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        program = vao = 0;
        layout = null;
        layoutModel = null;
        lastLighting = null;
        dirty.clear();
        worldEpoch = -1L;
        invalidate();
    }
}
