package com.siirio.jemluxium.client;

import com.siirio.jemluxium.JEMLuxium;
import com.siirio.jemluxium.config.LuxiumClientConfig;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.config.IrisConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
final class LuxiumShaderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JEMLuxium.MOD_ID);
    private static final String LUXIUM_PREFIX = "luxium ";

    private LuxiumShaderController() {
    }

    static boolean isLuxiumInstalled() {
        return findLuxiumPack().isPresent();
    }

    static boolean isLuxiumActive() {
        Optional<String> selectedPack = Iris.getIrisConfig().getShaderPackName();
        return IrisApi.getInstance().getConfig().areShadersEnabled()
                && selectedPack.filter(LuxiumShaderController::isLuxiumName).isPresent();
    }

    static void setLuxiumEnabled(boolean enabled) {
        if (enabled) {
            enableLuxium();
        } else {
            restoreOculusState();
        }
    }

    private static void enableLuxium() {
        Optional<String> luxiumPack = findLuxiumPack();
        if (luxiumPack.isEmpty()) {
            LOGGER.error("Cannot enable Luxium because no valid Luxium shader pack is installed");
            return;
        }

        IrisConfig irisConfig = Iris.getIrisConfig();
        String currentPack = irisConfig.getShaderPackName().orElse("");
        boolean shadersEnabled = IrisApi.getInstance().getConfig().areShadersEnabled();
        if (shadersEnabled && currentPack.equals(luxiumPack.get())) {
            return;
        }

        if (!isLuxiumName(currentPack)) {
            LuxiumClientConfig.remember(currentPack, shadersEnabled);
        } else if (!LuxiumClientConfig.HAS_PREVIOUS_OCULUS_STATE.get()) {
            LuxiumClientConfig.remember(currentPack, shadersEnabled);
        }

        irisConfig.setShaderPackName(luxiumPack.get());
        IrisApi.getInstance().getConfig().setShadersEnabledAndApply(true);
    }

    private static void restoreOculusState() {
        IrisConfig irisConfig = Iris.getIrisConfig();
        if (LuxiumClientConfig.HAS_PREVIOUS_OCULUS_STATE.get()) {
            String previousPack = LuxiumClientConfig.PREVIOUS_SHADER_PACK.get();
            if (!previousPack.isBlank()) {
                irisConfig.setShaderPackName(previousPack);
            }
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(
                    LuxiumClientConfig.PREVIOUS_SHADERS_ENABLED.get()
            );
            LuxiumClientConfig.clearRememberedState();
            return;
        }

        IrisApi.getInstance().getConfig().setShadersEnabledAndApply(false);
    }

    private static Optional<String> findLuxiumPack() {
        Path shaderpacksDirectory = Iris.getShaderpacksDirectory();
        try (Stream<Path> candidates = Files.list(shaderpacksDirectory)) {
            return candidates
                    .filter(Files::isRegularFile)
                    .filter(Iris::isValidShaderpack)
                    .map(path -> path.getFileName().toString())
                    .filter(LuxiumShaderController::isLuxiumName)
                    .sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
                    .findFirst();
        } catch (IOException exception) {
            LOGGER.error("Cannot inspect the Oculus shaderpacks directory for Luxium", exception);
            return Optional.empty();
        }
    }

    private static boolean isLuxiumName(String shaderPackName) {
        return shaderPackName.toLowerCase(Locale.ROOT).startsWith(LUXIUM_PREFIX);
    }
}
