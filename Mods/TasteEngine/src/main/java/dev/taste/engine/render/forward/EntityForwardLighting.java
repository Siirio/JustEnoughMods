package dev.taste.engine.render.forward;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import dev.taste.engine.environment.EnvironmentSnapshot;
import dev.taste.engine.light.ActorShadowPass;
import dev.taste.engine.light.ShadowRenderer;
import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import dev.taste.engine.material.MaterialResolver;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.PipelineParameters;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorMaterials;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Decorates known entity material layers inside the ordinary native dispatcher draw. */
public final class EntityForwardLighting {
    /** Custom renderers may opt in when their base model uses standard entity buffers. */
    public interface StandardLayerAdapter { }

    private record Context(FrameSnapshot frame, MaterialResolver materials, boolean useWetness, boolean armorReflections,
            boolean useRim, boolean emission, boolean adapterOnly, boolean reactiveFlashes, float rain) { }
    private record Wet(float amount, double time) { }
    private record Armor(ResourceLocation texture, ArmorMaterials material) { }
    private record Appearance(float roughness, float metallic, float f0, float subsurface, float emission,
            float porosity, boolean skin, int tint, Integer emissionTint) { }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_WET_ACTORS = 1024;
    private static final int MAX_SHADOW_CASCADES = 4;
    private static final Map<UUID, Wet> WET_ACTORS = new LinkedHashMap<>(32, 0.75F, true);
    private static final EntityForwardTypes TYPES = new EntityForwardTypes();

    private static Map<RenderType, Armor> armorTypes;
    private static Context current;
    private static ShaderInstance shader;
    private static String unavailable = "";
    private static long worldEpoch = -1L;
    private static boolean failed;
    private static boolean reactiveOverlay;

    private EntityForwardLighting() { }

    public static void begin(FrameSnapshot frame, PipelineParameters parameters, EnvironmentSnapshot environment,
            MaterialResolver materials, Vector3f towardsLight, Vector3f lightColor, float lightIntensity) {
        RenderSystem.assertOnRenderThread();
        current = null;
        reactiveOverlay = false;
        if (!enabled(parameters, "entities.enhancedLighting")) {
            if (shader != null || failed) release();
            return;
        }
        if (frame.worldEpoch() != worldEpoch) {
            WET_ACTORS.clear();
            worldEpoch = frame.worldEpoch();
        }
        if (shader == null && !failed) {
            try {
                shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                        new ResourceLocation("taste", "taste_entity_forward"), DefaultVertexFormat.NEW_ENTITY);
                setUniforms(frame, parameters, environment, towardsLight, lightColor, lightIntensity);
            } catch (IOException | RuntimeException failure) {
                if (shader != null) {
                    shader.close();
                    shader = null;
                }
                unavailable = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
                failed = true;
                LOGGER.warn("Taste entity material shader unavailable; preserving native actor batches", failure);
            }
        } else if (shader != null) {
            setUniforms(frame, parameters, environment, towardsLight, lightColor, lightIntensity);
        }
        if (shader == null) return;
        boolean useWetness = enabled(parameters, "entities.wetness");
        if (!useWetness && !WET_ACTORS.isEmpty()) WET_ACTORS.clear();
        current = new Context(frame, materials, useWetness, enabled(parameters, "entities.armorReflections"),
                enabled(parameters, "entities.rimLight"), enabled(parameters, "emission.enabled"),
                parameters.value("entities.unknownPolicy").getAsString().equals("adapter_only"),
                flashScale(parameters) > 0.0F, environment.rain());
    }

    private static void setUniforms(FrameSnapshot frame, PipelineParameters parameters, EnvironmentSnapshot environment,
            Vector3f towardsLight, Vector3f lightColor, float lightIntensity) {
        Matrix4f view = new Matrix4f().set(frame.view().columnMajor());
        Vector3f viewLight = view.transformDirection(new Vector3f(towardsLight));
        if (viewLight.lengthSquared() > 1.0e-8F) viewLight.normalize();
        else viewLight.set(0.0F, 1.0F, 0.0F);
        require("WorldLightView").set(viewLight);
        require("InverseFrameView").set(view.invert());
        require("EntityCascadeCount").set(0);
        for (int index = 0; index < MAX_SHADOW_CASCADES; index++) shader.setSampler("EntityShadow" + index, 0);
        require("WorldLightColor").set(lightColor);
        require("WorldLightStrength").set(Math.max(0.0F, lightIntensity));
        require("WorldSkyColor").set(linearColor(parameters.value("sky.zenithColor").getAsString()));
        require("WorldSkyStrength").set(environment.hasSky() ? value(parameters, "lighting.skyIntensity") : 0.0F);
        require("SkinScatter").set(value(parameters, "entities.skinScattering"));
        require("RimStrength").set(value(parameters, "entities.rimStrength"));
        require("RimWidth").set(value(parameters, "entities.rimWidth"));
        require("FlashStrength").set(flashScale(parameters));
    }

    public static void directionalShadows(List<ShadowRenderer.MapLayer> maps, FrameSnapshot frame,
            PipelineParameters parameters) {
        if (current == null || shader == null || current.frame() != frame) return;
        int count = enabled(parameters, "entities.shadowsOnSelf") && enabled(parameters, "shadows.enabled")
                ? Math.min(MAX_SHADOW_CASCADES, maps.size()) : 0;
        require("EntityCascadeCount").set(count);
        int fallback = count == 0 ? 0 : maps.get(0).texture();
        for (int index = 0; index < MAX_SHADOW_CASCADES; index++) {
            ShadowRenderer.MapLayer layer = index < count ? maps.get(index) : null;
            shader.setSampler("EntityShadow" + index, layer == null ? fallback : layer.texture());
            if (layer == null) continue;
            require("EntityShadowProjection" + index).set(layer.view().projection());
            require("EntityShadowFar" + index).set(layer.view().far());
        }
        require("EntityShadowBias").set(value(parameters, "shadows.bias"));
        require("EntityShadowNormalBias").set(value(parameters, "shadows.normalBias"));
        int samples = parameters.value("shadows.filter").getAsString().equals("hard") ? 1
                : Math.min(9, parameters.value("shadows.filterSamples").getAsInt());
        require("EntityShadowSamples").set(samples);
    }

    public static MultiBufferSource wrap(Entity entity, MultiBufferSource buffers) {
        return ActorMotionPass.wrap(entity, shade(entity, buffers));
    }

    private static MultiBufferSource shade(Entity entity, MultiBufferSource buffers) {
        Context active = current;
        if (active == null || buffers == null || !RenderSystem.isOnRenderThread() || ActorShadowPass.replaying()) return buffers;
        EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) return buffers;
        boolean vanillaRenderer = renderer.getClass().getName().startsWith("net.minecraft.client.renderer.entity.");
        boolean adapter = renderer instanceof StandardLayerAdapter;
        if (!vanillaRenderer && !adapter) return buffers;
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean vanillaEntity = entityId != null && entityId.getNamespace().equals("minecraft");
        ResourceLocation texture = renderer.getTextureLocation(entity);
        if (texture == null) return buffers;
        MaterialDescription body = active.materials().resolve(MaterialQuery.entity(entity.getType(), texture, MaterialQuery.Layer.ENTITY));
        boolean explicitlyDeclared = !body.equals(MaterialDescription.fallback());
        if (!vanillaEntity && !adapter && (active.adapterOnly() || !explicitlyDeclared)) return buffers;
        float wet = active.useWetness() ? wetness(entity, active) : 0.0F;

        boolean playerOrDeclaredTranslucency = entity.getType() == EntityType.PLAYER || (adapter && explicitlyDeclared);
        return type -> {
            if (current != active) return buffers.getBuffer(type);
            EntityForwardTypes.Layer bodyLayer = EntityForwardTypes.bodyLayer(type, texture, playerOrDeclaredTranslucency);
            if (bodyLayer != null) {
                return wrapLayer(buffers, type, texture, bodyLayer, appearance(body), wet, active);
            }
            Armor armor = armorTypes().get(type);
            if (armor == null) return buffers.getBuffer(type);
            MaterialDescription material = active.materials().resolve(
                    MaterialQuery.entity(entity.getType(), armor.texture(), MaterialQuery.Layer.ENTITY));
            Appearance look = material.equals(MaterialDescription.fallback()) ? armorAppearance(armor.material()) : appearance(material);
            return wrapLayer(buffers, type, armor.texture(), EntityForwardTypes.Layer.ARMOR, look, wet, active);
        };
    }

    private static VertexConsumer wrapLayer(MultiBufferSource buffers, RenderType original,
            ResourceLocation texture, EntityForwardTypes.Layer layer, Appearance look, float wet, Context active) {
        RenderType variant = TYPES.get(texture, layer, look.tint(), look.emissionTint(), () -> {
            if (shader == null) return null;
            require("SurfaceTint").set(linearColor(look.tint()));
            require("EmissionTint").set(look.emissionTint() == null ? new Vector3f(-1.0F) : linearColor(look.emissionTint()));
            return shader;
        });
        if (variant == null) return buffers.getBuffer(original);
        boolean armor = layer == EntityForwardTypes.Layer.ARMOR;
        float surfaceWetness = wet * (0.55F + 0.45F * Math.max(look.porosity(), look.metallic()));
        return new EntityMaterialVertices(buffers.getBuffer(variant), look.roughness(), look.metallic(), look.f0(),
                surfaceWetness, look.subsurface(), active.emission() ? look.emission() : 0.0F, look.skin(), layer.cutout(),
                active.useRim(), armor && active.armorReflections());
    }

    private static float wetness(Entity entity, Context active) {
        UUID id = entity.getUUID();
        Wet previous = WET_ACTORS.get(id);
        double now = active.frame().timeSeconds();
        if (previous != null && now == previous.time()) return previous.amount();
        boolean submerged = entity.isInWaterOrBubble();
        boolean raining = active.rain() > 0.02F && (entity.level().isRainingAt(entity.blockPosition())
                || entity.level().isRainingAt(entity.blockPosition().above()));
        float target = submerged ? 1.0F : raining ? active.rain() : 0.0F;
        float elapsed = previous == null ? 0.05F : (float) Math.max(0.0, Math.min(12.0, now - previous.time()));
        float before = previous == null ? 0.0F : previous.amount();
        float rate = submerged ? 2.5F : raining ? 0.65F : 0.16F;
        float amount = target + (before - target) * (float) Math.exp(-elapsed * rate);
        if (amount < 0.001F && target <= 0.0F) {
            WET_ACTORS.remove(id);
            return 0.0F;
        }
        WET_ACTORS.put(id, new Wet(amount, now));
        if (WET_ACTORS.size() > MAX_WET_ACTORS) WET_ACTORS.remove(WET_ACTORS.keySet().iterator().next());
        return amount;
    }

    private static Map<RenderType, Armor> armorTypes() {
        if (armorTypes != null) return armorTypes;
        armorTypes = new IdentityHashMap<>();
        for (ArmorMaterials material : ArmorMaterials.values()) {
            for (int layer = 1; layer <= 2; layer++) {
                ResourceLocation texture = new ResourceLocation("minecraft",
                        "textures/models/armor/" + material.getName() + "_layer_" + layer + ".png");
                armorTypes.put(RenderType.armorCutoutNoCull(texture), new Armor(texture, material));
            }
        }
        return armorTypes;
    }

    private static Appearance appearance(MaterialDescription material) {
        return new Appearance(material.roughness(), material.metallic(), material.f0(), material.subsurface(),
                material.emission(), material.porosity(), material.metadata().family() == MaterialDescription.Family.SKIN,
                material.tintRgb(), material.metadata().emissionColorRgb());
    }

    private static Appearance armorAppearance(ArmorMaterials material) {
        return switch (material) {
            case LEATHER -> armor(0.85F, 0.0F, 0.04F, 0.9F);
            case CHAIN -> armor(0.48F, 0.78F, 0.07F, 0.1F);
            case IRON -> armor(0.38F, 0.86F, 0.08F, 0.06F);
            case GOLD -> armor(0.26F, 0.94F, 0.16F, 0.04F);
            case DIAMOND -> armor(0.25F, 0.12F, 0.10F, 0.08F);
            case TURTLE -> armor(0.69F, 0.0F, 0.05F, 0.3F);
            case NETHERITE -> armor(0.43F, 0.74F, 0.11F, 0.05F);
        };
    }

    private static Appearance armor(float roughness, float metallic, float f0, float porosity) {
        return new Appearance(roughness, metallic, f0, 0.0F, 0.0F, porosity, false, 0xFFFFFF, null);
    }

    static void markOverlay(int u, int v) {
        if (current != null && current.reactiveFlashes() && ((u & 15) > 0 || (v & 15) == 3))
            reactiveOverlay = true;
    }

    public static boolean hasReactiveOverlay() { return reactiveOverlay; }

    public static String unavailableReason() { return unavailable; }

    public static void end() { current = null; }

    public static void release() {
        current = null;
        reactiveOverlay = false;
        WET_ACTORS.clear();
        TYPES.clear();
        armorTypes = null;
        worldEpoch = -1L;
        failed = false;
        unavailable = "";
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }

    private static Uniform require(String name) {
        Uniform uniform = shader.getUniform(name);
        if (uniform == null) throw new IllegalStateException("Missing entity forward uniform " + name);
        return uniform;
    }

    private static float flashScale(PipelineParameters parameters) {
        return Math.min(value(parameters, "entities.damageFlash"), value(parameters, "accessibility.flashLimit"));
    }

    private static boolean enabled(PipelineParameters parameters, String id) { return parameters.value(id).getAsBoolean(); }

    private static float value(PipelineParameters parameters, String id) { return parameters.value(id).getAsFloat(); }

    private static Vector3f linearColor(String value) {
        return linearColor(Integer.parseInt(value.substring(1), 16));
    }

    private static Vector3f linearColor(int rgb) {
        return new Vector3f(linear(rgb >> 16), linear(rgb >> 8), linear(rgb));
    }

    private static float linear(int value) {
        float srgb = (value & 255) / 255.0F;
        return srgb <= 0.04045F ? srgb / 12.92F : (float) Math.pow((srgb + 0.055F) / 1.055F, 2.4);
    }
}
