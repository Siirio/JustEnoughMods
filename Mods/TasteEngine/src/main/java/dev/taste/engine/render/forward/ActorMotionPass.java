package dev.taste.engine.render.forward;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import dev.taste.engine.light.ActorShadowPass;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.GlStateScope;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

/** Rasterizes motion from the exact current and previous submitted native model vertices. */
public final class ActorMotionPass {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TARGET = "actor-motion";
    private static final int MAX_ACTORS = 96;
    private static final int MAX_FRAME_VERTICES = 196_608;
    private static final int MAX_BATCH_VERTICES = 16_384;
    private static final int VERTEX_FLOATS = 6;
    private static final int DRAW_FLOATS = 9;
    private static final int[] TRIANGLE_ORDER = {0, 1, 2, 0, 2, 3};

    private record FrameCapture(FrameSnapshot frame, Map<UUID, Actor> actors) { }
    private record Uniforms(int currentProjection, int currentUnjittered, int previousUnjittered,
            int worldDepth, int opaqueDepth, int texture, int cutout, int validity) {
        static Uniforms read(int program) {
            return new Uniforms(require(program, "CurrentProjection"), require(program, "CurrentUnjittered"),
                    require(program, "PreviousUnjittered"), require(program, "WorldDepth"),
                    require(program, "OpaqueDepth"), require(program, "BodyTexture"),
                    require(program, "Cutout"), require(program, "Validity"));
        }
    }

    private static final class Actor {
        private final ResourceLocation texture;
        private final Class<?> renderer;
        private final FrameSnapshot.Vec3d root;
        private final boolean reactive;
        private final Map<EntityForwardTypes.Layer, Batch> batches = new LinkedHashMap<>();
        private boolean overflow;

        Actor(ResourceLocation texture, Class<?> renderer, FrameSnapshot.Vec3d root, boolean reactive) {
            this.texture = texture;
            this.renderer = renderer;
            this.root = root;
            this.reactive = reactive;
        }
    }

    private static final class Batch {
        private float[] data = new float[VERTEX_FLOATS * 128];
        private int vertices;

        void add(float x, float y, float z, float u, float v, float alpha) {
            int offset = vertices * VERTEX_FLOATS;
            if (offset + VERTEX_FLOATS > data.length) {
                data = Arrays.copyOf(data, Math.min(MAX_BATCH_VERTICES * VERTEX_FLOATS, data.length * 2));
            }
            data[offset] = x;
            data[offset + 1] = y;
            data[offset + 2] = z;
            data[offset + 3] = u;
            data[offset + 4] = v;
            data[offset + 5] = alpha;
            vertices++;
        }

        int quads() { return vertices / 4; }
    }

    private static FrameCapture collecting;
    private static FrameCapture complete;
    private static FrameCapture previous;
    private static float partialTick;
    private static int frameVertices;
    private static int program;
    private static int vao;
    private static int vbo;
    private static Uniforms uniforms;
    private static boolean failed;

    private ActorMotionPass() { }

    public static void begin(FrameSnapshot frame, float currentPartialTick) {
        RenderSystem.assertOnRenderThread();
        collecting = failed ? null : new FrameCapture(frame, new LinkedHashMap<>());
        complete = null;
        partialTick = currentPartialTick;
        frameVertices = 0;
    }

    public static void end() {
        if (collecting != null) {
            complete = collecting;
            collecting = null;
        }
    }

    static MultiBufferSource wrap(Entity entity, MultiBufferSource buffers) {
        FrameCapture frame = collecting;
        if (frame == null || buffers == null || !RenderSystem.isOnRenderThread() || ActorShadowPass.replaying()
                || frame.actors().size() >= MAX_ACTORS || frameVertices >= MAX_FRAME_VERTICES) return buffers;
        EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null || !renderer.getClass().getName().startsWith("net.minecraft.client.renderer.entity.")) return buffers;
        ResourceLocation texture = renderer.getTextureLocation(entity);
        if (texture == null) return buffers;
        UUID id = entity.getUUID();
        double x = Mth.lerp((double) partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp((double) partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp((double) partialTick, entity.zOld, entity.getZ());
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return buffers;
        boolean reactive = entity instanceof LivingEntity living && (living.hurtTime > 0 || living.deathTime > 0);
        Actor actor = new Actor(texture, renderer.getClass(), new FrameSnapshot.Vec3d(x, y, z), reactive);
        if (frame.actors().putIfAbsent(id, actor) != null) {
            frame.actors().get(id).overflow = true;
            return buffers;
        }
        boolean player = entity.getType() == EntityType.PLAYER;
        return type -> {
            VertexConsumer target = buffers.getBuffer(type);
            if (collecting != frame || actor.overflow) return target;
            EntityForwardTypes.Layer layer = EntityForwardTypes.bodyLayer(type, texture, player);
            if (layer == null || layer == EntityForwardTypes.Layer.CUTOUT_OFFSET
                    || layer == EntityForwardTypes.Layer.CUTOUT_OFFSET_NO_OUTLINE)
                return target;
            Batch batch = actor.batches.computeIfAbsent(layer, ignored -> new Batch());
            return new CaptureVertices(frame, target, actor, batch, new Matrix4f(RenderSystem.getModelViewMatrix()));
        };
    }

    /** XY is unjittered current-minus-previous UV. Z is positive previous view depth. A is validity. */
    public static TextureTarget render(TargetPool targets, RenderTarget world, RenderTarget opaque, FrameSnapshot frame) {
        RenderSystem.assertOnRenderThread();
        FrameCapture current = complete;
        complete = null;
        if (failed || current == null || current.frame() != frame || world == null) return null;
        FrameCapture history = consecutive(current, previous) ? previous : null;
        previous = current;
        if (current.actors().values().stream().noneMatch(ActorMotionPass::hasGeometry)) {
            targets.release(TARGET);
            return null;
        }

        try (GlStateScope ignored = GlStateScope.capture()) {
            prepare();
            TextureTarget output = targets.acquire(new TargetDescriptor(TARGET, world.width, world.height,
                    TargetDescriptor.Format.RGBA32F, false, 1, 1, 1));
            clear(output);
            draw(current, history, world, opaque == null ? world : opaque);
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR)
                throw new IllegalStateException("Actor motion pass failed with OpenGL error 0x" + Integer.toHexString(error));
            return output;
        } catch (IOException | RuntimeException failure) {
            failed = true;
            invalidate();
            closeGpu();
            targets.release(TARGET);
            LOGGER.warn("Taste actor motion unavailable; actor history remains reactive", failure);
            return null;
        }
    }

    private static void draw(FrameCapture current, FrameCapture history, RenderTarget world, RenderTarget opaque) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(true, true, true, true);
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        int oldArray = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            bindTexture(0, world.getDepthTextureId());
            bindTexture(1, opaque.getDepthTextureId());
            GL20.glUniform1i(uniforms.worldDepth(), 0);
            GL20.glUniform1i(uniforms.opaqueDepth(), 1);
            GL20.glUniform1i(uniforms.texture(), 2);
            setMatrix(uniforms.currentProjection(), current.frame().projection());
            setMatrix(uniforms.currentUnjittered(), current.frame().unjitteredProjection());
            setMatrix(uniforms.previousUnjittered(), history == null ? current.frame().unjitteredProjection()
                    : history.frame().unjitteredProjection());
            Matrix4f inverseCurrent = matrix(current.frame().view()).invert();
            Matrix4f inverseOld = history == null ? inverseCurrent : matrix(history.frame().view()).invert();

            for (Map.Entry<UUID, Actor> entry : current.actors().entrySet()) {
                Actor actor = entry.getValue();
                if (!hasGeometry(actor)) continue;
                Actor old = history == null ? null : history.actors().get(entry.getKey());
                boolean identity = compatible(actor, old);
                bindTexture(2, Minecraft.getInstance().getTextureManager().getTexture(actor.texture).getId());
                for (Map.Entry<EntityForwardTypes.Layer, Batch> section : actor.batches.entrySet()) {
                    Batch batch = section.getValue();
                    if (batch.quads() == 0 || batch.vertices % 4 != 0) continue;
                    Batch oldBatch = identity ? old.batches.get(section.getKey()) : null;
                    boolean valid = oldBatch != null && stable(batch, oldBatch, actor, old, current.frame(),
                            history.frame(), inverseCurrent, inverseOld);
                    drawBatch(section.getKey(), batch, valid ? oldBatch : batch, valid);
                }
            }
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldArray);
        }
    }

    private static void drawBatch(EntityForwardTypes.Layer layer, Batch batch, Batch previousBatch, boolean valid) {
        int vertices = batch.quads() * TRIANGLE_ORDER.length;
        FloatBuffer packed = MemoryUtil.memAllocFloat(vertices * DRAW_FLOATS);
        try {
            for (int quad = 0; quad < batch.quads(); quad++) {
                for (int corner : TRIANGLE_ORDER) {
                    int index = (quad * 4 + corner) * VERTEX_FLOATS;
                    packed.put(batch.data, index, 3);
                    packed.put(previousBatch.data, index, 3);
                    packed.put(batch.data, index + 3, 3);
                }
            }
            packed.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, packed, GL15.GL_STREAM_DRAW);
            if (layer == EntityForwardTypes.Layer.SOLID || layer == EntityForwardTypes.Layer.CUTOUT)
                RenderSystem.enableCull();
            else RenderSystem.disableCull();
            GL20.glUniform1i(uniforms.cutout(), layer.cutout() ? 1 : 0);
            GL20.glUniform1f(uniforms.validity(), valid ? 1.0F : -1.0F);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices);
        } finally {
            MemoryUtil.memFree(packed);
        }
    }

    private static boolean stable(Batch current, Batch old, Actor actor, Actor prior, FrameSnapshot frame,
            FrameSnapshot oldFrame, Matrix4f inverseCurrent, Matrix4f inverseOld) {
        if (current.vertices != old.vertices) return false;
        float[] root = actor.root.relativeTo(frame.origin());
        float[] oldRoot = prior.root.relativeTo(oldFrame.origin());
        Vector3f point = new Vector3f();
        Vector3f oldPoint = new Vector3f();
        for (int vertex = 0; vertex < current.vertices; vertex++) {
            int index = vertex * VERTEX_FLOATS;
            if (Math.abs(current.data[index + 3] - old.data[index + 3]) > 0.00001F
                    || Math.abs(current.data[index + 4] - old.data[index + 4]) > 0.00001F
                    || Math.abs(current.data[index + 5] - old.data[index + 5]) > 0.025F) return false;
            point.set(current.data[index], current.data[index + 1], current.data[index + 2]);
            oldPoint.set(old.data[index], old.data[index + 1], old.data[index + 2]);
            inverseCurrent.transformPosition(point).sub(root[0], root[1], root[2]);
            inverseOld.transformPosition(oldPoint).sub(oldRoot[0], oldRoot[1], oldRoot[2]);
            if (point.distanceSquared(oldPoint) > 4.0F) return false;
        }
        return true;
    }

    private static boolean compatible(Actor current, Actor old) {
        if (old == null || old.overflow || current.reactive || old.reactive || current.renderer != old.renderer
                || !current.texture.equals(old.texture) || !current.batches.keySet().equals(old.batches.keySet())) return false;
        float[] delta = current.root.relativeTo(old.root);
        return delta[0] * delta[0] + delta[1] * delta[1] + delta[2] * delta[2] <= 16.0F * 16.0F;
    }

    private static boolean hasGeometry(Actor actor) {
        return !actor.overflow && actor.batches.values().stream().anyMatch(batch -> batch.quads() > 0);
    }

    private static boolean consecutive(FrameCapture current, FrameCapture old) {
        return old != null && old.frame().worldEpoch() == current.frame().worldEpoch()
                && old.frame().frameIndex() + 1 == current.frame().frameIndex()
                && old.frame().width() == current.frame().width() && old.frame().height() == current.frame().height()
                && current.frame().timeSeconds() >= old.frame().timeSeconds()
                && current.frame().timeSeconds() - old.frame().timeSeconds() <= 0.5;
    }

    private static void clear(TextureTarget target) {
        target.bindWrite(true);
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        float[] color = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, color);
        try {
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        } finally {
            RenderSystem.clearColor(color[0], color[1], color[2], color[3]);
        }
    }

    private static void prepare() throws IOException {
        if (program != 0) return;
        int oldArray = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        int vertex = compile(resources, GL20.GL_VERTEX_SHADER, "taste_actor_motion.vert");
        int fragment = 0;
        try {
            fragment = compile(resources, GL20.GL_FRAGMENT_SHADER, "taste_actor_motion.frag");
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
                throw new IOException("Actor motion link failed: " + GL20.glGetProgramInfoLog(program));
            uniforms = Uniforms.read(program);
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            attribute(0, 3, 0);
            attribute(1, 3, 3);
            attribute(2, 2, 6);
            attribute(3, 1, 8);
        } catch (IOException | RuntimeException failure) {
            closeGpu();
            throw failure;
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldArray);
            GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
        }
    }

    private static void attribute(int index, int size, int offset) {
        GL20.glEnableVertexAttribArray(index);
        GL20.glVertexAttribPointer(index, size, GL11.GL_FLOAT, false, DRAW_FLOATS * Float.BYTES,
                (long) offset * Float.BYTES);
    }

    private static int compile(ResourceManager resources, int type, String file) throws IOException {
        String source;
        try (var input = resources.getResourceOrThrow(new ResourceLocation("taste", "shaders/motion/" + file)).open()) {
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE) return shader;
        String error = GL20.glGetShaderInfoLog(shader);
        GL20.glDeleteShader(shader);
        throw new IOException(file + " compile failed: " + error);
    }

    private static int require(int owner, String name) {
        int uniform = GL20.glGetUniformLocation(owner, name);
        if (uniform < 0) throw new IllegalStateException("Missing actor motion uniform " + name);
        return uniform;
    }

    private static void setMatrix(int uniform, FrameSnapshot.Matrix4 matrix) {
        try (var stack = MemoryStack.stackPush()) {
            GL20.glUniformMatrix4fv(uniform, false, matrix(matrix).get(stack.mallocFloat(16)));
        }
    }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 source) { return new Matrix4f().set(source.columnMajor()); }

    private static void bindTexture(int unit, int id) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
        RenderSystem.bindTexture(id);
    }

    public static void invalidate() {
        collecting = complete = previous = null;
        frameVertices = 0;
    }

    public static void releaseTargets(TargetPool targets) {
        invalidate();
        targets.release(TARGET);
    }

    public static void release() {
        invalidate();
        closeGpu();
        failed = false;
    }

    private static void closeGpu() {
        if (program != 0) GL20.glDeleteProgram(program);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        if (vbo != 0) GL15.glDeleteBuffers(vbo);
        program = vao = vbo = 0;
        uniforms = null;
    }

    private static final class CaptureVertices implements VertexConsumer {
        private final FrameCapture frame;
        private final VertexConsumer target;
        private final Actor actor;
        private final Batch batch;
        private final Matrix4f modelView;
        private final Vector3f position = new Vector3f();
        private float u, v, alpha = 1.0F;

        CaptureVertices(FrameCapture frame, VertexConsumer target, Actor actor, Batch batch, Matrix4f modelView) {
            this.frame = frame;
            this.target = target;
            this.actor = actor;
            this.batch = batch;
            this.modelView = modelView;
        }

        @Override public VertexConsumer vertex(double x, double y, double z) {
            position.set((float) x, (float) y, (float) z);
            target.vertex(x, y, z);
            return this;
        }

        @Override public VertexConsumer color(int r, int g, int b, int a) {
            alpha = a / 255.0F;
            target.color(r, g, b, a);
            return this;
        }

        @Override public VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            target.uv(u, v);
            return this;
        }

        @Override public VertexConsumer overlayCoords(int u, int v) { target.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { target.uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { target.normal(x, y, z); return this; }

        @Override public void endVertex() {
            target.endVertex();
            if (collecting != frame || actor.overflow) return;
            if (frameVertices >= MAX_FRAME_VERTICES || batch.vertices >= MAX_BATCH_VERTICES) {
                actor.overflow = true;
                return;
            }
            modelView.transformPosition(position);
            batch.add(position.x, position.y, position.z, u, v, alpha);
            frameVertices++;
        }

        @Override public void defaultColor(int r, int g, int b, int a) {
            alpha = a / 255.0F;
            target.defaultColor(r, g, b, a);
        }

        @Override public void unsetDefaultColor() {
            alpha = 1.0F;
            target.unsetDefaultColor();
        }
    }
}
