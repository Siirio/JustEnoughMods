package com.siirio.jemluxium.client;

import com.siirio.jemluxium.JEMLuxium;
import com.siirio.jemluxium.config.LuxiumClientConfig;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.mixin.GameRendererAccessor;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@OnlyIn(Dist.CLIENT)
final class LuxiumShaderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JEMLuxium.MOD_ID);
    private static boolean previousOculusEnabled;
    private static boolean oculusStateInitialized;
    private static net.minecraft.client.multiplayer.ClientLevel observedLevel;
    private static int diagnosticTicks;
    private static boolean observedEnabled;
    private static int deferredWorldReloadTicks = -1;

    private LuxiumShaderController() {
    }

    static boolean isLuxiumActive() {
        return Config.CLIENT.luxiumEnabled.get();
    }

    static void setLuxiumEnabled(boolean enabled) {
        if (enabled) {
            enableLuxium();
        } else {
            restoreOculusState();
        }
    }

    static void reconcileOculusState() {
        boolean oculusEnabled = IrisApi.getInstance().getConfig().areShadersEnabled();
        observeRendererState(oculusEnabled);
        if (!oculusStateInitialized) {
            oculusStateInitialized = true;
            previousOculusEnabled = oculusEnabled;
            if (oculusEnabled && Config.CLIENT.luxiumEnabled.get()) {
                disableLuxiumForOculus();
            }
            return;
        }
        if (oculusEnabled && !previousOculusEnabled && Config.CLIENT.luxiumEnabled.get()) {
            disableLuxiumForOculus();
        }
        previousOculusEnabled = oculusEnabled;
    }

    private static void observeRendererState(boolean oculusEnabled) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean enabled = isLuxiumActive();
        if (observedLevel != minecraft.level || observedEnabled != enabled) {
            observedLevel = minecraft.level;
            observedEnabled = enabled;
            diagnosticTicks = 40;
            if (minecraft.level != null && enabled && !oculusEnabled) {
                deferredWorldReloadTicks = 20;
            } else {
                deferredWorldReloadTicks = -1;
            }
        }
        if (deferredWorldReloadTicks > 0 && --deferredWorldReloadTicks == 0) {
            if (minecraft.level != null && enabled && !oculusEnabled) {
                LuxiumGpuShaderFeatures.refreshNow();
                reloadEmbeddiumRenderer("stable world reconciliation");
                diagnosticTicks = 40;
            }
            deferredWorldReloadTicks = -1;
        }
        if (minecraft.level == null || diagnosticTicks == 0 || --diagnosticTicks != 0) return;
        var sky = NeoSkyCelestia.get();
        var frame = sky.frameState();
        LOGGER.info("Luxium world state: master={}, oculus={}, skyFeature={}, skyMode={}, frame={}, nearGeneration={}, farGeneration={}, entityShadows={}",
                enabled, IrisApi.getInstance().getConfig().areShadersEnabled(),
                LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled(), sky.isGpuModeSelected(), frame.enabled(),
                frame.nearGeneration(), frame.farGeneration(), Config.CLIENT.skyEntityShadowsEnabled.get());
    }

    private static void disableLuxiumForOculus() {
        Config.CLIENT.luxiumEnabled.set(false);
        Config.CLIENT_SPEC.save();
        LuxiumClientConfig.clearRememberedState();
        LuxiumGpuShaderFeatures.refreshNow();
        reloadEmbeddiumRenderer("Oculus ownership");
        LOGGER.info("Oculus shaders enabled and Luxium disabled");
    }

    private static void enableLuxium() {
        if (Config.CLIENT.luxiumEnabled.get()) {
            return;
        }

        boolean shadersEnabled = IrisApi.getInstance().getConfig().areShadersEnabled();
        if (!LuxiumClientConfig.HAS_PREVIOUS_OCULUS_STATE.get()) {
            LuxiumClientConfig.remember(shadersEnabled);
        }

        if (shadersEnabled) {
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(false);
        }
        Config.CLIENT.luxiumEnabled.set(true);
        Config.CLIENT_SPEC.save();
        previousOculusEnabled = false;
        LuxiumGpuShaderFeatures.refreshNow();
        reloadEmbeddiumRenderer("Luxium ownership");
        LOGGER.info("Luxium enabled and Oculus shaders suspended");
    }

    private static void restoreOculusState() {
        Config.CLIENT.luxiumEnabled.set(false);
        Config.CLIENT_SPEC.save();

        if (LuxiumClientConfig.HAS_PREVIOUS_OCULUS_STATE.get()) {
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(
                    LuxiumClientConfig.PREVIOUS_SHADERS_ENABLED.get()
            );
            previousOculusEnabled = LuxiumClientConfig.PREVIOUS_SHADERS_ENABLED.get();
            LuxiumClientConfig.clearRememberedState();
        }
        LuxiumGpuShaderFeatures.refreshNow();
        reloadEmbeddiumRenderer("restored Oculus ownership");
        LOGGER.info("Luxium disabled and the previous Oculus shader state restored");
    }

    private static void reloadEmbeddiumRenderer(String reason) {
        Minecraft minecraft = Minecraft.getInstance();
        // Native Luxium settings reload core entity shaders as well as terrain:
        // their resource selection depends on the GPU feature mask at load time.
        ((GameRendererAccessor) minecraft.gameRenderer).luxium$reloadShaders(minecraft.getResourceManager());
        if (minecraft.levelRenderer != null) {
            minecraft.levelRenderer.allChanged();
        }
        LOGGER.info("Luxium renderer applied ({}): master={}, sky={}, local={}, vanilla={}, oculus={}",
                reason,
                Config.CLIENT.luxiumEnabled.get(), LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled(),
                LuxiumGpuShaderFeatures.localReceiverEnabled(), LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled(),
                IrisApi.getInstance().getConfig().areShadersEnabled());
    }
}
