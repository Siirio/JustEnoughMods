package com.siirio.jematmosphere;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;

public final class AtmosphereOptions {
    private static final String KEY = "screen.jem_atmosphere.settings.";

    private AtmosphereOptions() {
    }

    public static void register() {
        OptionGUIConstructionEvent.BUS.addListener(event -> pages().forEach(event::addPage));
    }

    public static Screen open(Screen parent) {
        return new EmbeddiumVideoOptionsScreen(parent, pages());
    }

    private static List<OptionPage> pages() {
        var storage = new Storage();
        var master = option(Boolean.class, storage, "enabled", AtmosphereSettings.Options::setEnabled,
                settings -> settings.enabled)
                .setControl(TickBoxControl::new)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build();
        BooleanSupplier effects = master::getValue;
        var general = group("general")
                .add(master)
                .add(toggle(storage, "fog", (settings, value) -> settings.fog = value, settings -> settings.fog, effects))
                .add(toggle(storage, "sky_colors", (settings, value) -> settings.skyColors = value,
                        settings -> settings.skyColors, effects))
                .add(toggle(storage, "aurora", (settings, value) -> settings.aurora = value,
                        settings -> settings.aurora, effects))
                .add(toggle(storage, "rainbow", (settings, value) -> settings.rainbow = value,
                        settings -> settings.rainbow, effects))
                .add(toggle(storage, "distant_lightning", (settings, value) -> settings.distantLightning = value,
                        settings -> settings.distantLightning, effects))
                .build();
        var air = group("air")
                .add(toggle(storage, "wind", (settings, value) -> settings.wind = value,
                        settings -> settings.wind, effects))
                .add(toggle(storage, "river_mist", (settings, value) -> settings.riverMist = value,
                        settings -> settings.riverMist, effects))
                .add(toggle(storage, "snow_gust", (settings, value) -> settings.snowGust = value,
                        settings -> settings.snowGust, effects))
                .add(toggle(storage, "cave_air", (settings, value) -> settings.caveAir = value,
                        settings -> settings.caveAir, effects))
                .add(toggle(storage, "heat_shimmer", (settings, value) -> settings.heatShimmer = value,
                        settings -> settings.heatShimmer, effects))
                .add(toggle(storage, "underwater_haze", (settings, value) -> settings.underwaterHaze = value,
                        settings -> settings.underwaterHaze, effects))
                .build();
        var particles = group("particles")
                .add(toggle(storage, "canopy_leaves", (settings, value) -> settings.canopyLeaves = value,
                        settings -> settings.canopyLeaves, effects))
                .add(toggle(storage, "campfire_smoke", (settings, value) -> settings.campfireSmoke = value,
                        settings -> settings.campfireSmoke, effects))
                .add(toggle(storage, "dark_campfire_smoke", (settings, value) -> settings.darkSmoke = value,
                        settings -> settings.darkSmoke, () -> effects.getAsBoolean() && storage.getData().campfireSmoke))
                .add(option(String.class, storage, "wind_colors", (settings, value) -> settings.windColors = value,
                        settings -> settings.windColors)
                        .setEnabledPredicate(() -> effects.getAsBoolean() && storage.getData().wind)
                        .setControl(option -> new WindColorsControl(option, () -> storage.getData().windColors))
                        .build())
                .build();
        return List.of(new OptionPage(OptionIdentifier.create(JemAtmosphere.ID, "atmosphere"), label("page"),
                ImmutableList.of(general, air, particles)));
    }

    private static OptionGroup.Builder group(String name) {
        return OptionGroup.createBuilder().setId(new ResourceLocation(JemAtmosphere.ID, name));
    }

    private static <T> OptionImpl.Builder<AtmosphereSettings.Options, T> option(Class<T> type, Storage storage,
            String key, BiConsumer<AtmosphereSettings.Options, T> setter,
            Function<AtmosphereSettings.Options, T> getter) {
        return OptionImpl.createBuilder(type, storage)
                .setId(OptionIdentifier.create(JemAtmosphere.ID, key, type))
                .setName(label(key))
                .setTooltip(label(key + ".tooltip"))
                .setBinding(setter, getter);
    }

    private static OptionImpl<AtmosphereSettings.Options, Boolean> toggle(Storage storage, String key,
            BiConsumer<AtmosphereSettings.Options, Boolean> setter,
            Function<AtmosphereSettings.Options, Boolean> getter, BooleanSupplier available) {
        return option(Boolean.class, storage, key, setter, getter)
                .setEnabledPredicate(available)
                .setControl(TickBoxControl::new)
                .build();
    }

    private static Component label(String key, Object... arguments) {
        return Component.translatable(KEY + key, arguments);
    }

    private static final class Storage implements OptionStorage<AtmosphereSettings.Options> {
        private final AtmosphereSettings.Options data = new AtmosphereSettings.Options();

        @Override
        public AtmosphereSettings.Options getData() {
            return data;
        }

        @Override
        public void save() {
            data.save();
        }
    }
}
