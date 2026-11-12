package dev.taste.engine.render.forward;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.logging.LogUtils;
import dev.taste.engine.environment.LightningEvents;
import dev.taste.engine.light.ActorShadowPass;
import dev.taste.engine.render.GlStateScope;
import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Records radiance from recognized native layers without changing their visible draws. */
public final class NativeEmissionCapture {
    private enum Layer {
        NONE(""),
        EYES("rendertype_eyes"),
        LIGHTNING("rendertype_lightning"),
        GLINT("rendertype_glint"),
        DIRECT_GLINT("rendertype_glint_direct"),
        TRANSLUCENT_GLINT("rendertype_glint_translucent"),
        ENTITY_GLINT("rendertype_entity_glint"),
        DIRECT_ENTITY_GLINT("rendertype_entity_glint_direct"),
        ARMOR_GLINT("rendertype_armor_glint"),
        ARMOR_ENTITY_GLINT("rendertype_armor_entity_glint");

        private final String program;
        Layer(String program) { this.program = program; }
        boolean armor() { return this == ARMOR_GLINT || this == ARMOR_ENTITY_GLINT; }
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TARGET = "native-layer-emission";
    private static final Map<Integer, RenderTarget> SOURCES = new HashMap<>();
    private static final Map<ShaderInstance, Layer> NATIVE_SHADERS = new IdentityHashMap<>();
    private static TextureTarget capture;
    private static ShaderInstance eyes;
    private static ShaderInstance glint;
    private static ShaderInstance lightning;
    private static float eyesStrength;
    private static float glintStrength;
    private static float lightningStrength;
    private static int attachedDepth;
    private static boolean active;
    private static boolean captured;
    private static boolean failed;

    private NativeEmissionCapture() { }

    public static void begin(TargetPool targets, RenderTarget world, PipelineParameters parameters) {
        RenderSystem.assertOnRenderThread();
        end();
        captured = false;
        float strength = parameters.value("emission.enabled").getAsBoolean()
                ? parameters.value("emission.strength").getAsFloat() : 0.0F;
        eyesStrength = parameters.value("emission.eyes").getAsBoolean() ? strength : 0.0F;
        glintStrength = strength * parameters.value("emission.enchantedGlint").getAsFloat();
        lightningStrength = LightningEvents.effectiveStrength(parameters);
        if (failed || world == null || world.getDepthTextureId() <= 0
                || (eyesStrength <= 0.0F && glintStrength <= 0.0F && lightningStrength <= 0.0F)) {
            releaseTargets(targets);
            return;
        }
        try (GlStateScope ignored = GlStateScope.capture()) {
            prepare();
            capture = targets.acquire(new TargetDescriptor(TARGET, world.width, world.height,
                    TargetDescriptor.Format.RGBA32F, false, 1, 1, 1));
            capture.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            float[] prior = new float[4];
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, prior);
            try {
                RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            } finally {
                RenderSystem.clearColor(prior[0], prior[1], prior[2], prior[3]);
            }
            active = true;
            admit(world);
        } catch (IOException | RuntimeException failure) {
            markFailure(failure);
            releaseTargets(targets);
        }
    }

    /** Adds an explicitly owned same-size target in the current world view. */
    public static void admit(RenderTarget source) {
        if (active && source != null && source.frameBufferId > 0 && source.getDepthTextureId() > 0
                && source.width == capture.width && source.height == capture.height) {
            SOURCES.put(source.frameBufferId, source);
        }
    }

    private static void prepare() throws IOException {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        if (eyesStrength > 0.0F && eyes == null) {
            eyes = new ShaderInstance(resources, new ResourceLocation("taste", "taste_native_eyes_emission"),
                    DefaultVertexFormat.NEW_ENTITY);
        }
        if (glintStrength > 0.0F && glint == null) {
            glint = new ShaderInstance(resources, new ResourceLocation("taste", "taste_native_glint_emission"),
                    DefaultVertexFormat.POSITION_TEX);
        }
        if (lightningStrength > 0.0F && lightning == null) {
            lightning = new ShaderInstance(resources, new ResourceLocation("taste", "taste_native_lightning_emission"),
                    DefaultVertexFormat.POSITION_COLOR);
        }
    }

    public static void replay(VertexBuffer mesh, Matrix4f modelView, Matrix4f projection, ShaderInstance nativeShader) {
        if (!active || nativeShader == null || ActorShadowPass.replaying() || mesh.isInvalid()) return;
        Layer layer = NATIVE_SHADERS.computeIfAbsent(nativeShader, NativeEmissionCapture::nativeLayer);
        float strength = switch (layer) {
            case NONE -> 0.0F;
            case EYES -> eyesStrength;
            case LIGHTNING -> lightningStrength;
            default -> glintStrength;
        };
        if (strength <= 0.0F) return;
        RenderTarget source = SOURCES.get(GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
        if (source == null) return;
        boolean untextured = layer == Layer.LIGHTNING;
        int nativeTexture = untextured ? 0 : RenderSystem.getShaderTexture(0);
        if (!untextured && nativeTexture <= 0) return;
        ShaderInstance semantic = switch (layer) {
            case EYES -> eyes;
            case LIGHTNING -> lightning;
            default -> glint;
        };
        try (GlStateScope ignored = GlStateScope.capture()) {
            capture.bindWrite(false);
            attachDepth(source.getDepthTextureId());
            if (!untextured) semantic.setSampler("Sampler0", nativeTexture);
            uniform(semantic, "ModelViewMat").set(modelView);
            uniform(semantic, "ProjMat").set(projection);
            uniform(semantic, "IViewRotMat").set(RenderSystem.getInverseViewRotationMatrix());
            uniform(semantic, "ColorModulator").set(RenderSystem.getShaderColor());
            uniform(semantic, "FogStart").set(RenderSystem.getShaderFogStart());
            uniform(semantic, "FogEnd").set(RenderSystem.getShaderFogEnd());
            uniform(semantic, "FogShape").set(RenderSystem.getShaderFogShape().getIndex());
            uniform(semantic, "EmissionStrength").set(strength);
            if (semantic == glint) {
                uniform(semantic, "TextureMat").set(RenderSystem.getTextureMatrix());
                uniform(semantic, "GlintAlpha").set(RenderSystem.getShaderGlintAlpha());
                uniform(semantic, "ArmorFog").set(layer.armor() ? 1 : 0);
            }
            semantic.apply();
            try {
                RenderSystem.depthMask(false);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
                // Nested native lightning shells share one capped flash.
                int radianceBlend = untextured ? GL14.GL_MAX : GL14.GL_FUNC_ADD;
                GL20.glBlendEquationSeparate(radianceBlend, GL14.GL_MIN);
                mesh.bind();
                mesh.draw();
                requireNoCaptureError();
                captured = true;
            } finally {
                semantic.clear();
                // Reapply the native BlendMode before returning to the native RenderType.
                nativeShader.apply();
                nativeShader.clear();
            }
        } catch (RuntimeException failure) {
            markFailure(failure);
        }
    }

    private static void attachDepth(int depth) {
        if (attachedDepth == depth) return;
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depth, 0);
        attachedDepth = depth;
        if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Native layer emission depth attachment is incomplete");
    }

    private static Layer nativeLayer(ShaderInstance shader) {
        Layer candidate;
        if (shader == GameRenderer.getRendertypeEyesShader()) candidate = Layer.EYES;
        else if (shader == GameRenderer.getRendertypeLightningShader()) candidate = Layer.LIGHTNING;
        else if (shader == GameRenderer.getRendertypeGlintShader()) candidate = Layer.GLINT;
        else if (shader == GameRenderer.getRendertypeGlintDirectShader()) candidate = Layer.DIRECT_GLINT;
        else if (shader == GameRenderer.getRendertypeGlintTranslucentShader()) candidate = Layer.TRANSLUCENT_GLINT;
        else if (shader == GameRenderer.getRendertypeEntityGlintShader()) candidate = Layer.ENTITY_GLINT;
        else if (shader == GameRenderer.getRendertypeEntityGlintDirectShader()) candidate = Layer.DIRECT_ENTITY_GLINT;
        else if (shader == GameRenderer.getRendertypeArmorGlintShader()) candidate = Layer.ARMOR_GLINT;
        else if (shader == GameRenderer.getRendertypeArmorEntityGlintShader()) candidate = Layer.ARMOR_ENTITY_GLINT;
        else return Layer.NONE;
        return originalResources(candidate) ? candidate : Layer.NONE;
    }

    private static boolean originalResources(Layer candidate) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        for (String extension : new String[] {".json", ".vsh", ".fsh"}) {
            var resource = resources.getResource(new ResourceLocation("minecraft",
                    "shaders/core/" + candidate.program + extension));
            if (resource.isEmpty() || !resource.get().sourcePackId().equals("vanilla")) return false;
        }
        return true;
    }

    /** RGB is linear source radiance. Alpha is the closest recorded native fragment depth. */
    public static TextureTarget target() { return captured ? capture : null; }

    public static void end() {
        active = false;
        SOURCES.clear();
        if (attachedDepth == 0) return;
        int prior = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            if (capture != null && capture.frameBufferId > 0) {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capture.frameBufferId);
                GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prior);
            attachedDepth = 0;
        }
    }

    public static void releaseTargets(TargetPool targets) {
        end();
        targets.release(TARGET);
        captured = false;
        capture = null;
    }

    public static void release() {
        end();
        captured = false;
        capture = null;
        failed = false;
        NATIVE_SHADERS.clear();
        if (eyes != null) {
            eyes.close();
            eyes = null;
        }
        if (glint != null) {
            glint.close();
            glint = null;
        }
        if (lightning != null) {
            lightning.close();
            lightning = null;
        }
    }

    private static void requireNoCaptureError() {
        int error = GL11.glGetError();
        if (error == GL11.GL_NO_ERROR) return;
        for (int attempt = 0; attempt < 16 && GL11.glGetError() != GL11.GL_NO_ERROR; attempt++) {
            // Drain capture errors before the native RenderType resumes.
        }
        throw new IllegalStateException("Native layer emission draw failed with OpenGL error 0x" + Integer.toHexString(error));
    }

    private static void markFailure(Exception failure) {
        if (!failed) LOGGER.warn("Taste native layer emission unavailable; preserving the native draw", failure);
        active = false;
        captured = false;
        failed = true;
    }

    private static Uniform uniform(ShaderInstance shader, String name) {
        Uniform uniform = shader.getUniform(name);
        if (uniform == null) throw new IllegalStateException("Missing native emission uniform " + name);
        return uniform;
    }
}
