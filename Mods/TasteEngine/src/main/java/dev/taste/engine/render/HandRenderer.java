package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import dev.taste.engine.material.MaterialResolver;
import dev.taste.engine.mixin.client.LevelRendererTransparencyAccessor;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Routes the native hand draw into its own native-resolution color and depth attachments. */
public final class HandRenderer implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FIRST = "hand-color-0";
    private static final String SECOND = "hand-color-1";
    private static final ThreadLocal<HandRenderer> ACTIVE = new ThreadLocal<>();

    public record Lighting(Vector3f viewDirection, Vector3f linearColor, float strength) {
        public Lighting {
            viewDirection = new Vector3f(viewDirection);
            linearColor = new Vector3f(linearColor);
        }
    }

    public record Reflection(MaterialResolver materials, Vector3f viewUp, Vector3f linearSky,
            Vector3f linearHorizon, float skyVisibility) {
        public Reflection {
            java.util.Objects.requireNonNull(materials);
            viewUp = new Vector3f(viewUp);
            linearSky = new Vector3f(linearSky);
            linearHorizon = new Vector3f(linearHorizon);
        }
    }

    private ShaderInstance shader;
    private GlStateScope nativeState;
    private AutoCloseable route;
    private RenderTarget nativeTarget;
    private TextureTarget handTarget;
    private TextureTarget historyTarget;
    private Matrix4f projection;
    private Matrix4f previousProjection;
    private PipelineParameters parameters;
    private Lighting light;
    private Reflection reflection;
    private ItemStack displayedMain;
    private ItemStack displayedOff;
    private boolean customHand;
    private long frame;
    private long previousFrame = -1;

    private int outputIndex;
    private boolean motion;
    private boolean depthOfField;
    private float emission;
    private ItemStack mainHand = ItemStack.EMPTY;
    private ItemStack offHand = ItemStack.EMPTY;
    private ItemStack previousMain = ItemStack.EMPTY;
    private ItemStack previousOff = ItemStack.EMPTY;
    private String failure = "";
    private AutoCloseable itemLayerRoute;

    public void invalidateHistory() { previousFrame = -1; }

    public void begin(TargetPool pool, long nextFrame, PipelineParameters options,
            Matrix4f handProjection, Lighting lighting) {
        begin(pool, nextFrame, options, handProjection, lighting, null);
    }

    public void begin(TargetPool pool, long nextFrame, PipelineParameters options,
            Matrix4f handProjection, Lighting lighting, Reflection reflectionInput) {
        if (capturing() || pool == null || options == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            release(pool);
            return;
        }

        boolean illuminated = lighting != null && lighting.strength() > 0
                && (enabled(options, "hand.enhancedLighting") || enabled(options, "hand.shadows"));
        depthOfField = enabled(options, "hand.depthOfField");
        motion = enabled(options, "hand.motionBlur") && !enabled(options, "accessibility.reducedMotion")
                && number(options, "camera.shutter") > 0;
        mainHand = minecraft.player.getMainHandItem();
        offHand = minecraft.player.getOffhandItem();
        emission = enabled(options, "hand.bloom") && enabled(options, "bloom.enabled") && offHand.isEmpty()
                ? emitter(mainHand) * number(options, "bloom.strength") : 0.0F;
        boolean canReflect = enabled(options, "hand.reflections") && reflectionInput != null
                && reflectionInput.skyVisibility() > 0 && mainHand.getItem() instanceof BlockItem && offHand.isEmpty();
        if (!illuminated && !depthOfField && !motion && emission <= 0.0F && !canReflect) {
            release(pool);
            return;
        }

        if (!motion) {
            historyTarget = null;
            outputIndex = 0;
            pool.release(SECOND);
        }
        frame = nextFrame;
        parameters = options;
        projection = new Matrix4f(handProjection);
        light = illuminated ? lighting : null;
        reflection = canReflect ? reflectionInput : null;
        displayedMain = null;
        displayedOff = null;
        customHand = false;
        nativeTarget = minecraft.getMainRenderTarget();

        try {
            if (shader == null) shader = new ShaderInstance(minecraft.getResourceManager(),
                    new ResourceLocation("taste", "taste_hand_composite"), DefaultVertexFormat.POSITION_TEX);
            nativeState = GlStateScope.capture();
            handTarget = pool.acquire(new TargetDescriptor(outputIndex == 0 ? FIRST : SECOND,
                    nativeTarget.width, nativeTarget.height, TargetDescriptor.Format.RGBA16F, true, 1, 1, 1));
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            handTarget.clear(Minecraft.ON_OSX);
            handTarget.bindWrite(true);
            route = WorldTargetRouter.route(handTarget);
            if (Minecraft.useShaderTransparency()) {
                var layers = (LevelRendererTransparencyAccessor) minecraft.levelRenderer;
                RenderTarget original = minecraft.levelRenderer.getItemEntityTarget();
                if (original != null) {
                    layers.taste$setItemEntityTarget(handTarget);
                    itemLayerRoute = () -> layers.taste$setItemEntityTarget(original);
                }
            }
            ACTIVE.set(this);
        } catch (IOException | RuntimeException exception) {
            reportFailure(exception);
            resetRoute();
            clearFrame();
        }
    }

    public void end(long expectedFrame, Runnable draw) {
        if (!capturing()) return;
        resetRoute();
        try {
            if (frame != expectedFrame) throw new IllegalStateException("Hand frame changed during capture");
            if (displayedMain != null) mainHand = displayedMain;
            if (displayedOff != null) offHand = displayedOff;
            if (customHand || displayedMain == null || !offHand.isEmpty()) emission = 0.0F;
            else if (enabled(parameters, "hand.bloom") && enabled(parameters, "bloom.enabled"))
                emission = emitter(displayedMain) * number(parameters, "bloom.strength");
            MaterialDescription reflective = reflectiveMaterial();
            boolean useHistory = !customHand && motion && historyTarget != null && previousFrame + 1 == frame
                    && previousProjection.equals(projection)
                    && handTarget.width == historyTarget.width && handTarget.height == historyTarget.height
                    && ItemStack.isSameItemSameTags(mainHand, previousMain)
                    && ItemStack.isSameItemSameTags(offHand, previousOff);
            try (GlStateScope ignored = GlStateScope.capture()) {
                nativeTarget.bindWrite(true);
                TemporalPostPass.setupFullscreen(shader);
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
                shader.setSampler("HandColor", handTarget.getColorTextureId());
                shader.setSampler("HandDepth", handTarget.getDepthTextureId());
                shader.setSampler("PreviousHand", (useHistory ? historyTarget : handTarget).getColorTextureId());
                shader.getUniform("HandProjection").set(projection);
                shader.getUniform("InverseHandProjection").set(new Matrix4f(projection).invert());
                shader.getUniform("LightDirection").set(light == null ? new Vector3f(0, 0, 1) : light.viewDirection());
                shader.getUniform("LightColor").set(light == null ? new Vector3f() : light.linearColor());
                set("LightStrength", light == null ? 0.0F : Math.min(0.32F, light.strength() * 0.16F));
                set("DirectionalLighting", enabled(parameters, "hand.enhancedLighting") ? 1.0F : 0.0F);
                set("SelfShadow", enabled(parameters, "hand.shadows") ? 1.0F : 0.0F);
                set("Aperture", number(parameters, "camera.aperture"));
                set("BlurRadius", depthOfField ? Math.min(14.0F, number(parameters, "camera.dofMaxRadius")) : 0.0F);
                set("FocalLength", Math.abs(projection.m11()) * 0.012F);
                set("Shutter", useHistory ? number(parameters, "camera.shutter") : 0.0F);
                set("MotionSamples", Math.min(16.0F, number(parameters, "camera.motionSamples")));
                set("HandEmission", emission);
                bindReflection(reflective);
                try {
                    draw.run();
                    // The retained world depth belongs to its own target. Native post can still read hand depth.
                    if (nativeTarget.useDepth && !nativeTarget.isStencilEnabled())
                        nativeTarget.copyDepthFrom(handTarget);
                    int error = GL11.glGetError();
                    if (error != GL11.GL_NO_ERROR) throw new IllegalStateException("Hand composition GL error " + error);
                } finally {
                    shader.clear();
                }
            }
            if (motion) {
                historyTarget = handTarget;
                previousProjection = projection;
                previousFrame = frame;
                previousMain = mainHand.copy();
                previousOff = offHand.copy();
                outputIndex ^= 1;
            }
            failure = "";
        } catch (RuntimeException exception) {
            reportFailure(exception);
            historyTarget = null;
        } finally {
            clearFrame();
        }
    }

    public static void observeNativeHand(InteractionHand hand, ItemStack item, boolean canceled) {
        HandRenderer current = ACTIVE.get();
        if (current == null) return;
        current.customHand |= canceled;
        if (hand == InteractionHand.MAIN_HAND) current.displayedMain = item;
        else current.displayedOff = item;
    }

    private MaterialDescription reflectiveMaterial() {
        if (reflection == null || customHand || displayedMain == null || !offHand.isEmpty()
                || !(displayedMain.getItem() instanceof BlockItem block)) return null;
        var state = block.getBlock().defaultBlockState();
        RenderType type = ItemBlockRenderTypes.getChunkRenderType(state);
        MaterialQuery.Layer layer = type == RenderType.translucent() ? MaterialQuery.Layer.TRANSLUCENT
                : type == RenderType.cutout() ? MaterialQuery.Layer.CUTOUT
                : type == RenderType.cutoutMipped() ? MaterialQuery.Layer.CUTOUT_MIPPED : MaterialQuery.Layer.SOLID;
        MaterialDescription material = reflection.materials().resolve(MaterialQuery.block(state, null, layer, -1));
        return material.metallic() > 0.03F || material.roughness() < 0.65F ? material : null;
    }

    private void bindReflection(MaterialDescription material) {
        shader.getUniform("EnvironmentUp").set(material == null ? new Vector3f(0, 1, 0) : reflection.viewUp());
        shader.getUniform("EnvironmentSky").set(material == null ? new Vector3f() : reflection.linearSky());
        shader.getUniform("EnvironmentHorizon").set(material == null ? new Vector3f() : reflection.linearHorizon());
        set("EnvironmentStrength", material == null ? 0.0F : Math.min(1.0F, reflection.skyVisibility()));
        set("ReflectRoughness", material == null ? 1.0F : material.roughness());
        set("ReflectMetallic", material == null ? 0.0F : material.metallic());
        set("ReflectF0", material == null ? 0.04F : material.f0());
    }

    private void resetRoute() {
        if (ACTIVE.get() == this) ACTIVE.remove();
        try {
            if (route != null) route.close();
        } catch (Exception exception) {
            reportFailure(exception);
        } finally {
            route = null;
            if (itemLayerRoute != null) {
                try { itemLayerRoute.close(); }
                catch (Exception exception) { reportFailure(exception); }
                finally { itemLayerRoute = null; }
            }
            GlStateScope state = nativeState;
            nativeState = null;
            if (state != null) {
                try { state.close(); }
                catch (RuntimeException exception) { reportFailure(exception); }
            }
        }
    }

    private void release(TargetPool pool) {
        pool.release(FIRST);
        pool.release(SECOND);
        historyTarget = null;
        previousFrame = -1;
        outputIndex = 0;
    }

    private void clearFrame() {
        nativeTarget = null;
        handTarget = null;
        parameters = null;
        light = null;
        reflection = null;
        displayedMain = null;
        displayedOff = null;
        projection = null;
    }

    private static float emitter(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem block) return block.getBlock().defaultBlockState().getLightEmission() / 15.0F;
        return 0.0F;
    }

    private static boolean enabled(PipelineParameters parameters, String id) { return parameters.value(id).getAsBoolean(); }
    private static float number(PipelineParameters parameters, String id) { return parameters.value(id).getAsFloat(); }
    private void set(String uniform, float value) { shader.getUniform(uniform).set(value); }

    private void reportFailure(Exception exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        if (!failure.equals(message)) LOGGER.warn("Taste could not render the separate hand layer", exception);
        failure = message;
    }

    public boolean capturing() { return nativeState != null && route != null; }
    public String failure() { return failure; }

    @Override public void close() {
        resetRoute();
        clearFrame();
        historyTarget = null;
        previousFrame = -1;
        outputIndex = 0;
        previousProjection = null;
        previousMain = ItemStack.EMPTY;
        previousOff = ItemStack.EMPTY;
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
