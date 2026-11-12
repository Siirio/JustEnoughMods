package dev.taste.engine.environment;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Integrates world-space cloud volumes and reconstructs them into the lit scene. */
public final class CloudRenderer implements AutoCloseable {
    private static final int LAYERS = 3;
    private static final int SHADOW_RESOLUTION = 256;
    private static final float SHADOW_SPAN = 2048.0F;
    private static final double FIELD_PERIOD = 4096.0;
    private static final String[] SHAPES = {"Layer1Shape", "Layer2Shape", "Layer3Shape"};
    private static final String[] TINTS = {"Layer1Tint", "Layer2Tint", "Layer3Tint"};
    private static final String[] OFFSETS = {"Layer1Offset", "Layer2Offset", "Layer3Offset"};
    private static final String[] MOTION = {"Layer1Motion", "Layer2Motion", "Layer3Motion"};

    private record Settings(int mode, float coverage, float density, float scale, float windX, float windZ,
            int raySteps, int lightSteps, float resolution, float silverLining, float history, List<CloudLayers.Layer> layers) { }
    private record History(FrameSnapshot frame, Settings settings, float rain, float thunder, float flash,
            Vector3f lightDirection, Vector3f lightColor, float intensity, boolean daylight,
            boolean underwater, boolean lava, TextureTarget target) { }
    private record ShadowField(FrameSnapshot frame, Settings settings, double x, double y, double z,
            Vector3f direction, float rain, float thunder, TextureTarget target) { }

    private final double[] driftX = new double[LAYERS];
    private final double[] driftZ = new double[LAYERS];
    private final double[] motionX = new double[LAYERS];
    private final double[] motionZ = new double[LAYERS];
    private long driftEpoch = -1L;
    private double driftTime;
    private int outputIndex;
    private ShaderInstance integration;
    private ShaderInstance composite;
    private History previous;
    private ShadowField shadow;
    private TextureTarget cloudTexture;

    public static boolean enabled(PipelineParameters parameters, EnvironmentSnapshot environment) {
        return parameters != null && environment != null && environment.hasSky()
                && parameters.value("clouds.enabled").getAsBoolean();
    }

    public void prepare() throws IOException {
        if (integration == null) integration = shader("taste_cloud_integrate");
        if (composite == null) composite = shader("taste_cloud_composite");
    }

    public TextureTarget render(TargetPool targets, RenderTarget sourceScene, RenderTarget sceneDepth,
            FrameSnapshot frame, PipelineParameters parameters, EnvironmentSnapshot environment,
            Vector3f towardsLight, Vector3f lightColor, float lightIntensity, boolean daylight,
            Runnable draw) throws IOException {
        if (!enabled(parameters, environment)) {
            releaseTargets(targets);
            return null;
        }
        Settings settings = settings(parameters);
        if (settings.density() <= 0.0F
                || settings.layers().stream().noneMatch(layer -> layer.coverage() > 0.0F && layer.density() > 0.0F)) {
            releaseTargets(targets);
            return null;
        }
        prepare();
        advanceDrift(frame, settings);
        var level = Minecraft.getInstance().level;
        float lightning = level == null ? 0.0F : Math.min(1.0F, level.getSkyFlashTime() / 2.0F);
        float flash = lightning * LightningEvents.effectiveStrength(parameters);
        int width = Math.max(1, Math.round(frame.width() * settings.resolution()));
        int height = Math.max(1, Math.round(frame.height() * settings.resolution()));
        boolean useHistory = acceptsHistory(frame, settings, environment, towardsLight, lightColor, lightIntensity,
                daylight, lightning, width, height);
        if (settings.history() <= 0.0F) {
            targets.release("cloud-volume-1");
            previous = null;
            outputIndex = 0;
        }
        TextureTarget volume = target(targets, "cloud-volume-" + outputIndex, width, height, true);
        float mediumExtinction = mediumExtinction(parameters, environment);
        drawVolume(volume, sceneDepth, frame, settings, environment, towardsLight, lightColor, lightIntensity,
                flash, mediumExtinction, useHistory, draw);
        cloudTexture = volume;
        previous = new History(frame, settings, environment.rain(), environment.thunder(), lightning,
                new Vector3f(towardsLight), new Vector3f(lightColor), lightIntensity, daylight,
                environment.underwater(), environment.lava(), volume);
        if (settings.history() > 0.0F) outputIndex ^= 1;

        float shadowStrength = 0.0F;
        if (daylight && lightIntensity > 0.0F && towardsLight.y > 0.06F
                && parameters.value("shadows.enabled").getAsBoolean()
                && parameters.value("shadows.clouds").getAsBoolean()) {
            drawShadows(targets, frame, settings, environment, towardsLight, draw);
            shadowStrength = Math.min(1.0F, (towardsLight.y - 0.06F) / 0.15F);
        } else {
            targets.release("cloud-shadow");
            shadow = null;
        }
        TextureTarget output = target(targets, "cloud-composite", frame.width(), frame.height(), false);
        drawComposite(output, sourceScene, sceneDepth, volume, frame, settings, shadowStrength, draw);
        return output;
    }

    public TextureTarget cloudTexture() { return cloudTexture; }
    public TextureTarget shadowTexture() { return shadow == null ? null : shadow.target(); }

    private void drawVolume(TextureTarget output, RenderTarget depth, FrameSnapshot frame, Settings settings,
            EnvironmentSnapshot environment, Vector3f direction, Vector3f lightColor, float intensity, float flash,
            float mediumExtinction, boolean useHistory, Runnable draw) {
        begin(output, integration, true);
        integration.setSampler("ViewDepth", depth.getDepthTextureId());
        integration.setSampler("HistoryColor", useHistory ? previous.target().getColorTextureId() : depth.getDepthTextureId());
        integration.setSampler("HistoryDistance", useHistory ? previous.target().getDepthTextureId() : depth.getDepthTextureId());
        bindField(frame, settings, environment, direction);
        integration.getUniform("ShadowPass").set(0);
        integration.getUniform("InverseProjection").set(matrix(frame.inverseProjection()));
        integration.getUniform("InverseView").set(matrix(frame.view()).invert());
        integration.getUniform("LightColor").set(lightColor);
        integration.getUniform("LightIntensity").set(intensity);
        integration.getUniform("Lightning").set(flash);
        integration.getUniform("MediumExtinction").set(mediumExtinction);
        integration.getUniform("JitterIndex").set(settings.history() > 0.0F ? (int) (frame.frameIndex() & 63) : 0);
        integration.getUniform("TemporalWeight").set(useHistory ? settings.history() : 0.0F);
        if (useHistory) {
            var old = previous.frame();
            float[] originShift = frame.origin().relativeTo(old.origin());
            Matrix4f oldView = matrix(old.view()).translate(originShift[0], originShift[1], originShift[2]);
            integration.getUniform("PreviousClip").set(matrix(old.projection()).mul(oldView));
            integration.getUniform("PreviousCamera").set(new Matrix4f(oldView).invert().getTranslation(new Vector3f()));
        }
        for (int index = 0; index < LAYERS; index++) {
            integration.getUniform(MOTION[index]).set((float) motionX[index], (float) motionZ[index]);
        }
        draw(integration, draw);
    }

    private void drawShadows(TargetPool targets, FrameSnapshot frame, Settings settings,
            EnvironmentSnapshot environment, Vector3f direction, Runnable draw) {
        var camera = frame.cameraWorld();
        double x = Math.floor(camera.x() / 32.0) * 32.0;
        double y = Math.floor(camera.y() / 32.0) * 32.0;
        double z = Math.floor(camera.z() / 32.0) * 32.0;
        if (shadow != null && shadow.frame().worldEpoch() == frame.worldEpoch()
                && shadow.settings().equals(settings) && shadow.x() == x && shadow.y() == y && shadow.z() == z
                && Math.abs(environment.rain() - shadow.rain()) < 0.03F
                && Math.abs(environment.thunder() - shadow.thunder()) < 0.03F
                && direction.dot(shadow.direction()) > 0.9999F
                && frame.timeSeconds() >= shadow.frame().timeSeconds()
                && (frame.frameIndex() - shadow.frame().frameIndex() < 4
                    || frame.timeSeconds() == shadow.frame().timeSeconds())) return;
        TextureTarget output = target(targets, "cloud-shadow", SHADOW_RESOLUTION, SHADOW_RESOLUTION, false);
        output.setFilterMode(GL11.GL_LINEAR);
        ShadowField next = new ShadowField(frame, settings, x, y, z, new Vector3f(direction),
                environment.rain(), environment.thunder(), output);
        begin(output, integration, false);
        // The shared program declares history/depth samplers. Bind valid non-output textures during the map pass.
        integration.setSampler("ViewDepth", cloudTexture.getDepthTextureId());
        integration.setSampler("HistoryColor", cloudTexture.getColorTextureId());
        integration.setSampler("HistoryDistance", cloudTexture.getDepthTextureId());
        bindField(frame, settings, environment, direction);
        integration.getUniform("ShadowPass").set(1);
        bindShadowProjection(integration, frame, next);
        draw(integration, draw);
        shadow = next;
    }

    private void drawComposite(TextureTarget output, RenderTarget source, RenderTarget depth, TextureTarget volume,
            FrameSnapshot frame, Settings settings, float shadowStrength, Runnable draw) {
        begin(output, composite, false);
        composite.setSampler("SceneColor", source.getColorTextureId());
        composite.setSampler("ViewDepth", depth.getDepthTextureId());
        composite.setSampler("CloudColor", volume.getColorTextureId());
        composite.setSampler("CloudDistance", volume.getDepthTextureId());
        composite.setSampler("CloudShadow", shadow == null ? volume.getColorTextureId() : shadow.target().getColorTextureId());
        composite.getUniform("InverseProjection").set(matrix(frame.inverseProjection()));
        composite.getUniform("InverseView").set(matrix(frame.view()).invert());
        composite.getUniform("ShadowStrength").set(shadowStrength);
        for (int index = 0; index < LAYERS; index++) {
            CloudLayers.Layer layer = settings.layers().get(index);
            composite.getUniform(SHAPES[index]).set(layer.baseHeight() - (float) frame.origin().y(), layer.thickness());
        }
        if (shadow != null) {
            bindShadowProjection(composite, frame, shadow);
            composite.getUniform("ShadowDirection").set(shadow.direction());
        }
        draw(composite, draw);
    }

    private void bindField(FrameSnapshot frame, Settings settings, EnvironmentSnapshot environment, Vector3f direction) {
        float frequency = settings.scale() / 192.0F;
        float weather = environment.rain() * 0.24F + environment.thunder() * 0.14F;
        integration.getUniform("CloudMode").set(settings.mode());
        integration.getUniform("CloudCoverage").set(settings.coverage());
        integration.getUniform("CloudDensity").set(settings.density());
        integration.getUniform("FieldFrequency").set(frequency);
        integration.getUniform("RainCoverage").set(weather);
        integration.getUniform("Storm").set(environment.rain() * 0.65F + environment.thunder() * 0.35F);
        integration.getUniform("LightDirection").set(direction);
        integration.getUniform("RaySteps").set(settings.raySteps());
        integration.getUniform("LightSteps").set(settings.lightSteps());
        integration.getUniform("SilverLining").set(settings.silverLining());
        int seed = frame.dimension().hashCode();
        for (int index = 0; index < LAYERS; index++) {
            CloudLayers.Layer layer = settings.layers().get(index);
            integration.getUniform(SHAPES[index]).set(layer.baseHeight() - (float) frame.origin().y(),
                    layer.thickness(), layer.coverage(), layer.density());
            integration.getUniform(TINTS[index]).set(layer.red(), layer.green(), layer.blue());
            double seedX = ((seed >>> 3) & 1023) + index * 239;
            double seedZ = ((seed >>> 13) & 1023) + index * 151;
            integration.getUniform(OFFSETS[index]).set(wrapped((frame.origin().x() - driftX[index]) * frequency + seedX),
                    wrapped((frame.origin().z() - driftZ[index]) * frequency + seedZ));
        }
    }

    private static void bindShadowProjection(ShaderInstance shader, FrameSnapshot frame, ShadowField field) {
        float[] center = new FrameSnapshot.Vec3d(field.x(), field.y(), field.z()).relativeTo(frame.origin());
        shader.getUniform("ShadowCenter").set(center);
        shader.getUniform("ShadowSpan").set(SHADOW_SPAN);
    }

    private boolean acceptsHistory(FrameSnapshot frame, Settings settings, EnvironmentSnapshot environment,
            Vector3f direction, Vector3f lightColor, float intensity, boolean daylight, float flash,
            int width, int height) {
        if (previous == null || settings.history() <= 0.0F || !settings.equals(previous.settings())) return false;
        FrameSnapshot old = previous.frame();
        float[] movement = frame.cameraWorld().relativeTo(old.cameraWorld());
        return old.worldEpoch() == frame.worldEpoch() && old.frameIndex() + 1 == frame.frameIndex()
                && previous.target().width == width && previous.target().height == height
                && frame.timeSeconds() >= old.timeSeconds() && frame.timeSeconds() - old.timeSeconds() <= 0.5
                && old.unjitteredProjection().equals(frame.unjitteredProjection())
                && movement[0] * movement[0] + movement[1] * movement[1] + movement[2] * movement[2] < 64.0F * 64.0F
                && Math.abs(environment.rain() - previous.rain()) < 0.08F
                && Math.abs(environment.thunder() - previous.thunder()) < 0.08F
                && previous.daylight() == daylight && direction.dot(previous.lightDirection()) > 0.995F
                && lightColor.distanceSquared(previous.lightColor()) < 0.01F
                && environment.underwater() == previous.underwater() && environment.lava() == previous.lava()
                && Math.abs(intensity - previous.intensity()) < 0.12F && flash == 0.0F && previous.flash() == 0.0F;
    }

    private void advanceDrift(FrameSnapshot frame, Settings settings) {
        boolean reset = frame.worldEpoch() != driftEpoch || frame.timeSeconds() < driftTime;
        double elapsed = reset ? frame.timeSeconds() : frame.timeSeconds() - driftTime;
        for (int index = 0; index < LAYERS; index++) {
            float speed = settings.layers().get(index).speed();
            double stepX = elapsed * settings.windX() * speed;
            double stepZ = elapsed * settings.windZ() * speed;
            driftX[index] = reset ? stepX : driftX[index] + stepX;
            driftZ[index] = reset ? stepZ : driftZ[index] + stepZ;
            motionX[index] = reset ? 0.0 : stepX;
            motionZ[index] = reset ? 0.0 : stepZ;
        }
        driftEpoch = frame.worldEpoch();
        driftTime = frame.timeSeconds();
    }

    private static float mediumExtinction(PipelineParameters parameters, EnvironmentSnapshot environment) {
        if (environment.lava()) return 1.0F / Math.max(0.1F, value(parameters, "lava.submergedVisibility"));
        if (environment.underwater() && parameters.value("underwater.fog").getAsBoolean())
            return value(parameters, "underwater.fogDensity");
        return 0.0F;
    }

    private static Settings settings(PipelineParameters parameters) {
        List<CloudLayers.Layer> layers = List.of(layer(parameters, "cloudLayer1."), layer(parameters, "cloudLayer2."),
                layer(parameters, "cloudLayer3."));
        int mode = switch (parameters.value("clouds.mode").getAsString()) {
            case "block" -> 0;
            case "soft" -> 1;
            default -> 2;
        };
        double windDirection = Math.toRadians(value(parameters, "clouds.windDirection"));
        float windSpeed = value(parameters, "clouds.windSpeed");
        return new Settings(mode, value(parameters, "clouds.coverage"), value(parameters, "clouds.density"),
                value(parameters, "clouds.scale"), (float) Math.cos(windDirection) * windSpeed,
                (float) Math.sin(windDirection) * windSpeed, parameters.value("clouds.raySteps").getAsInt(),
                parameters.value("clouds.lightSteps").getAsInt(),
                Float.parseFloat(parameters.value("clouds.resolutionScale").getAsString()),
                value(parameters, "clouds.silverLining"), value(parameters, "clouds.temporalWeight"), layers);
    }

    private static CloudLayers.Layer layer(PipelineParameters parameters, String prefix) {
        float density = parameters.value(prefix + "enabled").getAsBoolean() ? value(parameters, prefix + "density") : 0.0F;
        int color = Integer.parseInt(parameters.value(prefix + "color").getAsString().substring(1), 16);
        return new CloudLayers.Layer(value(parameters, prefix + "baseHeight"), value(parameters, prefix + "thickness"),
                value(parameters, prefix + "coverage"), density, value(parameters, prefix + "speed"),
                linear(color >> 16), linear(color >> 8), linear(color));
    }

    private static void begin(TextureTarget target, ShaderInstance shader, boolean writeDepth) {
        target.bindWrite(true);
        if (writeDepth) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(writeDepth);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> shader);
    }

    private static void draw(ShaderInstance shader, Runnable draw) {
        try {
            draw.run();
        } finally {
            shader.clear();
        }
    }

    private static TextureTarget target(TargetPool targets, String name, int width, int height, boolean depth) {
        return targets.acquire(new TargetDescriptor(name, width, height, TargetDescriptor.Format.RGBA16F,
                depth, 1, 1, 1));
    }

    private static ShaderInstance shader(String name) throws IOException {
        return new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                new ResourceLocation("taste", name), DefaultVertexFormat.POSITION_TEX);
    }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 elements) { return new Matrix4f().set(elements.columnMajor()); }
    private static float value(PipelineParameters parameters, String id) { return parameters.value(id).getAsFloat(); }
    private static float wrapped(double value) { return (float) (value - Math.floor(value / FIELD_PERIOD) * FIELD_PERIOD); }

    private static float linear(int channel) {
        float srgb = (channel & 255) / 255.0F;
        return srgb <= 0.04045F ? srgb / 12.92F : (float) Math.pow((srgb + 0.055F) / 1.055F, 2.4);
    }

    public void invalidate() {
        previous = null;
        shadow = null;
        cloudTexture = null;
        outputIndex = 0;
    }

    public void releaseTargets(TargetPool targets) {
        targets.release("cloud-volume-0");
        targets.release("cloud-volume-1");
        targets.release("cloud-shadow");
        targets.release("cloud-composite");
        invalidate();
    }

    @Override public void close() {
        invalidate();
        driftEpoch = -1L;
        if (integration != null) {
            integration.close();
            integration = null;
        }
        if (composite != null) {
            composite.close();
            composite = null;
        }
    }
}
