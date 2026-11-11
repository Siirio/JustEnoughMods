package dev.taste.engine.client;

import com.mojang.logging.LogUtils;
import dev.taste.engine.config.ConfigStore;
import dev.taste.engine.config.EffectiveConfig;
import dev.taste.engine.config.PresetCatalog;
import dev.taste.engine.config.SettingRegistry;
import java.io.IOException;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.ModList;
import dev.taste.engine.render.CapabilityProbe;
import dev.taste.engine.render.TasteRenderer;
import java.util.LinkedHashSet;
import java.util.Set;

/** Loaded only by Dist.CLIENT subscribers. Capability claims come from the active GL context. */
public final class ClientState {
    private static ConfigStore store;
    private ClientState() { }
    public static synchronized void initialize() {
        if (store != null) return;
        try {
            SettingRegistry registry = SettingRegistry.bundled();
            store = new ConfigStore(registry, PresetCatalog.bundled(registry), FMLPaths.CONFIGDIR.get().resolve("taste-client.json"));
            store.warnings().forEach(message -> LogUtils.getLogger().warn("Taste configuration: {}", message));
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Taste developer foundation could not load its validated registry", failure);
        }
    }
    public static ConfigStore store() { initialize(); return store; }

    public static boolean rendererAvailable() { return TasteRenderer.instance().baselineAvailable(); }
    public static boolean embeddiumInstalled() { return ModList.get().isLoaded("embeddium"); }
    public static EffectiveConfig.Capabilities capabilities() {
        EffectiveConfig.Embeddium embeddium = EffectiveConfig.Embeddium.ABSENT;
        if (embeddiumInstalled()) {
            String version = ModList.get().getModContainerById("embeddium")
                    .map(container -> container.getModInfo().getVersion().toString()).orElse("");
            embeddium = version.startsWith("0.3.31")
                    ? EffectiveConfig.Embeddium.SUPPORTED_0_3_31 : EffectiveConfig.Embeddium.UNSUPPORTED;
        }
        CapabilityProbe.Result result = TasteRenderer.instance().capabilityResult().orElse(null);
        Set<String> flags = result == null ? Set.of() : new LinkedHashSet<>(result.flags());
        return new EffectiveConfig.Capabilities(true, result != null && result.baseline(), embeddium, flags);
    }
}
