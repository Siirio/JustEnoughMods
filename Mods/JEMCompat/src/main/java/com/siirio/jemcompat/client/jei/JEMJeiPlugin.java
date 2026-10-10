package com.siirio.jemcompat.client.jei;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.config.IClientToggleState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.MinecraftForge;
import com.siirio.jemcompat.mixin.client.IngredientListOverlayAccessor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@JeiPlugin
public final class JEMJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = new ResourceLocation("jemcompat", "jei");
    private static final String ENGLISH_ALIASES = "/assets/jemcompat/jei_english_aliases.json";
    private static IJeiRuntime runtime;

    public JEMJeiPlugin() {
        MinecraftForge.EVENT_BUS.register(CuratedJeiOverlay.class);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime availableRuntime) {
        runtime = availableRuntime;
        CuratedJeiCatalog.rebuild(availableRuntime);
        setNativeOverlayVisible(false);
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        CuratedJeiCatalog.clear();
        CuratedJeiOverlay.clear();
    }

    public static boolean hasSearchFocus() {
        return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
    }

    public static IJeiRuntime runtime() {
        return runtime;
    }

    public static boolean isNativeOverlayVisible() {
        IClientToggleState state = toggleState();
        return state != null && state.isOverlayEnabled();
    }

    public static void setNativeOverlayVisible(boolean visible) {
        IClientToggleState state = toggleState();
        if (state != null && state.isOverlayEnabled() != visible) {
            state.toggleOverlayEnabled();
        }
    }

    private static IClientToggleState toggleState() {
        if (runtime == null || !(runtime.getIngredientListOverlay() instanceof IngredientListOverlayAccessor accessor)) {
            return null;
        }
        return accessor.jemcompat$getToggleState();
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        Map<String, String> aliases = readAliases();
        ForgeRegistries.ITEMS.getValues().forEach(item -> {
            String alias = aliases.get(item.getDescriptionId());
            if (alias != null && !alias.isBlank()) {
                registration.addAlias(item, alias);
            }
        });
    }

    private static Map<String, String> readAliases() {
        InputStream stream = JEMJeiPlugin.class.getResourceAsStream(ENGLISH_ALIASES);
        if (stream == null) {
            throw new IllegalStateException("Missing English JEI aliases");
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, TypeToken.getParameterized(Map.class, String.class, String.class).getType());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load English JEI aliases", exception);
        }
    }
}
