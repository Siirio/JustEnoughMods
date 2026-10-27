package com.siirio.jemluxium.client;

import com.siirio.jemluxium.JEMLuxium;
import com.siirio.jemluxium.config.LuxiumClientConfig;
import com.vinlanx.luxium.Config;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@OnlyIn(Dist.CLIENT)
final class LuxiumShaderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JEMLuxium.MOD_ID);

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
        LOGGER.info("Luxium enabled and Oculus shaders suspended");
    }

    private static void restoreOculusState() {
        Config.CLIENT.luxiumEnabled.set(false);
        Config.CLIENT_SPEC.save();

        if (LuxiumClientConfig.HAS_PREVIOUS_OCULUS_STATE.get()) {
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(
                    LuxiumClientConfig.PREVIOUS_SHADERS_ENABLED.get()
            );
            LuxiumClientConfig.clearRememberedState();
        }
        LOGGER.info("Luxium disabled and the previous Oculus shader state restored");
    }
}
