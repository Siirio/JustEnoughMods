package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.taste.engine.client.ClientEvents;
import dev.taste.engine.environment.EnvironmentSnapshot;
import dev.taste.engine.render.reflection.ReflectionCapture;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Depth-valid refraction and reflection for the material mesh pass. */
public final class MaterialCompositePass implements AutoCloseable {
    private static final String TARGET = "material-composite";
    private ShaderInstance shader;
    private long epoch = -1;
    private double lastRain = Double.NEGATIVE_INFINITY;
    private TextureTarget reflectionHistory;
    private FrameSnapshot reflectionFrame;
    private long reflectionRevision = -1;
    private int reflectionIndex;

    public TextureTarget render(TargetPool targets, TextureTarget scene, TextureTarget opaque, TextureTarget world, TextureTarget metadata,
            TextureTarget optics, FrameSnapshot frame, PipelineParameters parameters, EnvironmentSnapshot environment,
            ReflectionCapture.Result captures, Runnable draw) throws IOException {
        if (opaque == null || (metadata == null && !environment.underwater() && !environment.lava())) {
            targets.release(TARGET);
            releaseReflections(targets);
            return scene;
        }
        if (shader == null) {
            shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation("taste", "taste_material_composite"), DefaultVertexFormat.POSITION_TEX);
        }
        if (epoch != frame.worldEpoch()) {
            epoch = frame.worldEpoch();
            lastRain = Double.NEGATIVE_INFINITY;
        }
        if (environment.rain() > 0.02F) lastRain = frame.timeSeconds();
        double dryTime = Math.max(0.01, value(parameters, "glass.drySeconds"));
        float rainOnGlass = (float) Math.max(0.0, Math.min(1.0, 1 - (frame.timeSeconds() - lastRain) / dryTime));
        TextureTarget result = targets.acquire(new TargetDescriptor(TARGET, frame.width(), frame.height(),
                TargetDescriptor.Format.RGBA16F, false, 1, 1, 1));
        result.setFilterMode(GL11.GL_LINEAR);
        result.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("SceneSampler", scene.getColorTextureId());
        shader.setSampler("OpaqueColor", opaque.getColorTextureId());
        shader.setSampler("OpaqueDepth", opaque.getDepthTextureId());
        shader.setSampler("WorldDepth", world.getDepthTextureId());
        shader.setSampler("SurfaceData", metadata == null ? opaque.getColorTextureId() : metadata.getColorTextureId());
        shader.setSampler("SurfaceDepth", metadata == null ? opaque.getDepthTextureId() : metadata.getDepthTextureId());
        shader.setSampler("SurfaceOptics", optics == null ? opaque.getColorTextureId() : optics.getColorTextureId());
        set("HasMaterial", metadata == null ? 0 : 1);
        set("HasSurfaceOptics", optics == null ? 0 : 1);
        setMatrix("Projection", matrix(frame.projection()));
        setMatrix("InverseProjection", matrix(frame.inverseProjection()));
        Matrix4f view = matrix(frame.view());
        setMatrix("View", view);
        setMatrix("InverseView", new Matrix4f(view).invert());
        float wavelength = value(parameters, "water.waveLength");
        double phasePeriod = Math.max(wavelength, 0.01) * 2.0;
        shader.getUniform("WaveOrigin").set((float) (frame.origin().x() % phasePeriod),
                (float) (frame.origin().z() % phasePeriod));
        float time = (float) (frame.timeSeconds() % 4096.0);
        set("VisualTime", enabled(parameters, "accessibility.reducedMotion") ? 0 : time);
        set("WaterAbsorption", value(parameters, "water.absorption"));
        set("WaterClarity", value(parameters, "water.clarity"));
        set("WaterIor", value(parameters, "water.ior"));
        set("WaterRefraction", enabled(parameters, "water.refraction") ? value(parameters, "water.refractionStrength") : 0);
        set("WaterWaveHeight", value(parameters, "water.waveHeight"));
        set("WaterWaveLength", wavelength);
        set("WaterWaveSpeed", value(parameters, "water.waveSpeed"));
        set("WaterNormalStrength", value(parameters, "water.normalStrength"));
        set("WaterFlowStrength", value(parameters, "water.flowStrength"));
        set("WaterFoamWidth", enabled(parameters, "water.shoreFoam") ? value(parameters, "water.foamWidth") : 0);
        set("WaterFallFoam", enabled(parameters, "water.waterfallFoam") ? 1 : 0);
        bindRipples(frame, parameters, environment);
        set("GlassIor", value(parameters, "glass.ior"));
        set("GlassThickness", value(parameters, "glass.thickness"));
        set("GlassTransmission", value(parameters, "glass.stainedTransmission"));
        set("GlassRefraction", enabled(parameters, "glass.refraction") ? 1 : 0);
        set("GlassRain", enabled(parameters, "glass.rainDrops") ? rainOnGlass : 0);
        set("GlassDropDensity", value(parameters, "glass.dropDensity"));
        set("GlassDropSize", value(parameters, "glass.dropSize"));
        set("GlassDropSpeed", value(parameters, "glass.dropSpeed"));
        set("IceRoughness", value(parameters, "glass.iceRoughness"));
        set("IceFrost", enabled(parameters, "glass.frost") ? 1 : 0);
        set("LavaCrust", value(parameters, "lava.crust"));
        set("LavaWaveHeight", value(parameters, "lava.waveHeight"));
        set("LavaHeatDistortion", enabled(parameters, "lava.heatDistortion") ? value(parameters, "lava.distortionStrength") : 0);
        set("LavaVisibility", value(parameters, "lava.submergedVisibility"));
        set("EmissionStrength", enabled(parameters, "emission.enabled") ? value(parameters, "emission.strength") : 0);
        set("LavaEmission", enabled(parameters, "emission.lava") ? 1 : 0);
        setColor("SoulColor", parameters, "emission.soulColor");
        set("Underwater", environment.underwater() ? 1 : 0);
        set("CausticStrength", enabled(parameters, "underwater.caustics") ? value(parameters, "underwater.causticStrength") : 0);
        set("InLava", environment.lava() ? 1 : 0);
        set("Waterline", enabled(parameters, "underwater.waterline") ? 1 : 0);
        set("UnderwaterDistortion", value(parameters, "underwater.distortion"));
        boolean reflections = enabled(parameters, "reflections.enabled");
        String method = parameters.value("reflections.method").getAsString();
        set("ReflectionMode", reflections ? switch (method) { case "screen" -> 2; case "hybrid" -> 3; default -> 1; } : 0);
        set("ReflectionSteps", value(parameters, "reflections.steps"));
        set("ReflectionDistance", value(parameters, "reflections.maxDistance"));
        set("ReflectionThickness", value(parameters, "reflections.thickness"));
        set("ReflectionRoughness", value(parameters, "reflections.roughnessCutoff"));
        setColor("ShallowColor", parameters, "water.shallowColor");
        setColor("DeepColor", parameters, "water.deepColor");
        setColor("SkyColor", parameters, "sky.zenithColor");
        ReflectionCapture.bind(shader, captures, opaque);
        TextureTarget reflected = reflections && metadata != null
                ? renderReflection(targets, scene, opaque, frame, parameters, draw) : null;
        if (reflected == null) releaseReflections(targets);
        result.bindWrite(true);
        shader.setSampler("ReflectionSampler", reflected == null ? scene.getColorTextureId() : reflected.getColorTextureId());
        shader.setSampler("ReflectionPrevious", opaque.getColorTextureId());
        set("ReflectionActive", reflected == null ? 0 : 1);
        set("ReflectionOnly", 0);
        try {
            draw.run();
        } finally {
            shader.clear();
        }
        return result;
    }

    private TextureTarget renderReflection(TargetPool targets, TextureTarget scene, TextureTarget opaque,
            FrameSnapshot frame, PipelineParameters parameters, Runnable draw) {
        float scale = Float.parseFloat(parameters.value("reflections.resolutionScale").getAsString());
        int width = Math.max(1, Math.round(frame.width() * scale));
        int height = Math.max(1, Math.round(frame.height() * scale));
        TextureTarget output = targets.acquire(new TargetDescriptor("reflection-history-" + reflectionIndex,
                width, height, TargetDescriptor.Format.RGBA32F, false, 1, 1, 1));
        boolean reuse = reflectionHistory != null && reflectionFrame != null
                && reflectionRevision == parameters.revision() && reflectionFrame.worldEpoch() == frame.worldEpoch()
                && reflectionHistory.width == width && reflectionHistory.height == height
                && frame.timeSeconds() >= reflectionFrame.timeSeconds()
                && frame.unjitteredProjection().equals(reflectionFrame.unjitteredProjection());
        if (reuse) {
            FrameSnapshot.Vec3d now = frame.origin(), prior = reflectionFrame.origin();
            Matrix4f oldView = matrix(reflectionFrame.view()).translate((float) (now.x() - prior.x()),
                    (float) (now.y() - prior.y()), (float) (now.z() - prior.z()));
            Matrix4f oldProjection = matrix(reflectionFrame.projection()).mul(oldView);
            setMatrix("ReflectionPreviousProjection", oldProjection);
            setMatrix("ReflectionPreviousInverse", matrix(reflectionFrame.inverseProjection()));
        }
        output.bindWrite(true);
        shader.setSampler("ReflectionSampler", scene.getColorTextureId());
        shader.setSampler("ReflectionPrevious", reuse ? reflectionHistory.getColorTextureId() : opaque.getColorTextureId());
        set("ReflectionActive", 0);
        set("ReflectionOnly", 1);
        set("ReflectionHistoryWeight", reuse ? value(parameters, "reflections.temporalWeight") : 0);
        try {
            draw.run();
        } finally {
            shader.clear();
        }
        reflectionHistory = output;
        reflectionFrame = frame;
        reflectionRevision = parameters.revision();
        reflectionIndex ^= 1;
        return output;
    }

    private void releaseReflections(TargetPool targets) {
        targets.release("reflection-history-0");
        targets.release("reflection-history-1");
        invalidate();
    }

    public void invalidate() {
        reflectionHistory = null;
        reflectionFrame = null;
        reflectionRevision = -1;
        reflectionIndex = 0;
    }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 values) { return new Matrix4f().set(values.columnMajor()); }
    private void bindRipples(FrameSnapshot frame, PipelineParameters parameters, EnvironmentSnapshot environment) {
        int count = 0;
        FrameSnapshot.Vec3d origin = frame.origin();
        boolean entityRipples = enabled(parameters, "water.entityRipples");
        boolean rainRipples = enabled(parameters, "weather.ripples");
        for (var ripple : ClientEvents.waterRipples()) {
            if (count >= 8 || (ripple.entity() ? !entityRipples : !rainRipples)) continue;
            float age = (environment.gameTime() - ripple.bornTick()) / 20F;
            if (age < 0 || age > 1.2F) continue;
            int strength = Math.round(Math.min(4F, Math.max(0F, ripple.strength())) * 256);
            shader.getUniform("Ripple" + count).set((float) (ripple.x() - origin.x()), (float) (ripple.y() - origin.y()),
                    (float) (ripple.z() - origin.z()), strength * 2F + age);
            count++;
        }
        set("RippleCount", count);
    }
    private static boolean enabled(PipelineParameters parameters, String key) { return parameters.value(key).getAsBoolean(); }
    private static float value(PipelineParameters parameters, String key) { return parameters.value(key).getAsFloat(); }
    private void setMatrix(String name, Matrix4f value) { shader.getUniform(name).set(value); }
    private void set(String name, float value) { shader.getUniform(name).set(value); }
    private void setColor(String uniform, PipelineParameters parameters, String name) {
        int rgb = Integer.parseInt(parameters.value(name).getAsString().substring(1), 16);
        shader.getUniform(uniform).set(new Vector3f(linear(rgb >> 16), linear(rgb >> 8), linear(rgb)));
    }
    private static float linear(int channel) {
        float value = (channel & 255) / 255.0F;
        return value < 0.04045F ? value / 12.92F : (float) Math.pow((value + 0.055F) / 1.055F, 2.4);
    }
    @Override public void close() {
        invalidate();
        epoch = -1;
        lastRain = Double.NEGATIVE_INFINITY;
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
