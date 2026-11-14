package com.siirio.jemlightingsandbox;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;

public final class LightingOptions {
    private static final String MOD_ID = JemLightingSandbox.ID;
    private static final String KEY = "screen.jem_lighting_sandbox.settings.";

    private LightingOptions() {
    }

    public static void register() {
        OptionGUIConstructionEvent.BUS.addListener(event -> pages().forEach(event::addPage));
    }

    public static Screen open(Screen parent) {
        return new EmbeddiumVideoOptionsScreen(parent, pages());
    }

    private static List<OptionPage> pages() {
        var storage = new Storage();
        var master = option(Boolean.class, storage, "enabled", LightingSettings.Options::setEnabled,
                settings -> settings.enabled)
                .setControl(TickBoxControl::new)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build();
        var atmosphere = toggle(storage, "atmosphere", (settings, value) -> settings.atmosphere = value,
                settings -> settings.atmosphere, master::getValue);
        var followDistance = toggle(storage, "follow_render_distance",
                (settings, value) -> settings.followRenderDistance = value, settings -> settings.followRenderDistance,
                master::getValue);
        BooleanSupplier effects = () -> master.getValue() && atmosphere.getValue();
        var wind = toggle(storage, "wind", (settings, value) -> settings.wind = value,
                settings -> settings.wind, effects);

        var general = group("general").add(master).add(atmosphere).build();
        var lighting = group("lighting")
                .add(option(LightingSettings.Mode.class, storage, "mode", (settings, value) -> settings.mode = value,
                        settings -> settings.mode)
                        .setEnabledPredicate(master::getValue)
                        .setControl(option -> new CyclingControl<>(option, LightingSettings.Mode.class,
                                modeNames()))
                        .build())
                .add(followDistance)
                .add(slider(storage, "manual_distance", (settings, value) -> settings.chunkDistance = value,
                        settings -> settings.chunkDistance, () -> master.getValue() && !followDistance.getValue(),
                        LightingSettings.MIN_LIGHTING_CHUNKS, LightingSettings.MAX_LIGHTING_CHUNKS,
                        value -> label("chunks", value)))
                .add(slider(storage, "distance", (settings, value) -> settings.reachIndex = value,
                        settings -> settings.reachIndex, master::getValue, 0, LightingSettings.LIGHT_DISTANCES.size() - 1,
                        index -> label("blocks", LightingSettings.LIGHT_DISTANCES.get(index))))
                .add(slider(storage, "shadow_resolution", (settings, value) -> settings.shadowIndex = value,
                        settings -> settings.shadowIndex, master::getValue, 0, LightingSettings.SHADOW_RESOLUTIONS.size() - 1,
                        index -> label("pixels", LightingSettings.SHADOW_RESOLUTIONS.get(index))))
                .build();
        var weather = group("weather")
                .add(slider(storage, "rain_density", (settings, value) -> settings.rainDensity = value,
                        settings -> settings.rainDensity, effects, LightingSettings.MIN_WEATHER_DENSITY,
                        LightingSettings.MAX_WEATHER_DENSITY, ControlValueFormatter.percentage()))
                .add(slider(storage, "storm_density", (settings, value) -> settings.stormDensity = value,
                        settings -> settings.stormDensity, effects, LightingSettings.MIN_WEATHER_DENSITY,
                        LightingSettings.MAX_WEATHER_DENSITY, ControlValueFormatter.percentage()))
                .add(toggle(storage, "dark_campfire_smoke", (settings, value) -> settings.darkSmoke = value,
                        settings -> settings.darkSmoke, effects))
                .build();
        var visuals = group("effects")
                .add(wind)
                .add(toggle(storage, "river_mist", (settings, value) -> settings.riverMist = value,
                        settings -> settings.riverMist, effects))
                .add(toggle(storage, "snow_gust", (settings, value) -> settings.snowGust = value,
                        settings -> settings.snowGust, effects))
                .add(toggle(storage, "cave_air", (settings, value) -> settings.caveAir = value,
                        settings -> settings.caveAir, effects))
                .add(toggle(storage, "canopy_leaves", (settings, value) -> settings.canopyLeaves = value,
                        settings -> settings.canopyLeaves, effects))
                .add(option(String.class, storage, "wind_colors", (settings, value) -> settings.windColors = value,
                        settings -> settings.windColors)
                        .setEnabledPredicate(() -> effects.getAsBoolean() && wind.getValue())
                        .setControl(option -> new WindColorsControl(option, () -> storage.getData().windColors))
                        .build())
                .build();
        return List.of(
                new OptionPage(OptionIdentifier.create(MOD_ID, "lighting"), label("page"),
                        ImmutableList.of(general, lighting)),
                new OptionPage(OptionIdentifier.create(MOD_ID, "effects"), label("effects_page"),
                        ImmutableList.of(visuals, weather)));
    }

    private static OptionGroup.Builder group(String name) {
        return OptionGroup.createBuilder().setId(new ResourceLocation(MOD_ID, name));
    }

    private static <T> OptionImpl.Builder<LightingSettings.Options, T> option(Class<T> type, Storage storage,
            String key, BiConsumer<LightingSettings.Options, T> setter, Function<LightingSettings.Options, T> getter) {
        return OptionImpl.createBuilder(type, storage)
                .setId(OptionIdentifier.create(MOD_ID, key, type))
                .setName(label(key))
                .setTooltip(label(key + ".tooltip"))
                .setBinding(setter, getter);
    }

    private static OptionImpl<LightingSettings.Options, Boolean> toggle(Storage storage, String key,
            BiConsumer<LightingSettings.Options, Boolean> setter, Function<LightingSettings.Options, Boolean> getter,
            BooleanSupplier available) {
        return option(Boolean.class, storage, key, setter, getter).setEnabledPredicate(available)
                .setControl(TickBoxControl::new).build();
    }

    private static OptionImpl<LightingSettings.Options, Integer> slider(Storage storage, String key,
            BiConsumer<LightingSettings.Options, Integer> setter, Function<LightingSettings.Options, Integer> getter,
            BooleanSupplier available, int min, int max, ControlValueFormatter formatter) {
        return option(Integer.class, storage, key, setter, getter).setEnabledPredicate(available)
                .setControl(option -> new SliderControl(option, min, max, 1, formatter)).build();
    }

    private static Component[] modeNames() {
        var modes = LightingSettings.Mode.values();
        var names = new Component[modes.length];
        for (int index = 0; index < modes.length; index++) {
            names[index] = label("mode." + modes[index].name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static Component label(String key, Object... arguments) {
        return Component.translatable(KEY + key, arguments);
    }

    private static final class Storage implements OptionStorage<LightingSettings.Options> {
        private final LightingSettings.Options data = new LightingSettings.Options();

        @Override
        public LightingSettings.Options getData() {
            return data;
        }

        @Override
        public void save() {
            data.save();
        }
    }
}
