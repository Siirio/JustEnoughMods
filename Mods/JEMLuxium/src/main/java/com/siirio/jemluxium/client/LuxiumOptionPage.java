package com.siirio.jemluxium.client;

import com.google.common.collect.ImmutableList;
import com.siirio.jemluxium.JEMLuxium;
import com.vinlanx.luxium.client.ConfigScreen.LuxiumConfigScreen;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;

@OnlyIn(Dist.CLIENT)
final class LuxiumOptionPage extends OptionPage {
    LuxiumOptionPage() {
        super(
                OptionIdentifier.create(JEMLuxium.MOD_ID, "luxium"),
                Component.translatable("jem_luxium.options.page"),
                createGroups()
        );
    }

    private static ImmutableList<OptionGroup> createGroups() {
        LuxiumOptionStorage storage = new LuxiumOptionStorage();
        ActionOptionStorage actionStorage = new ActionOptionStorage();
        return ImmutableList.of(OptionGroup.createBuilder()
                .setId(new ResourceLocation(JEMLuxium.MOD_ID, "shader_selection"))
                .add(OptionImpl.createBuilder(Boolean.TYPE, storage)
                        .setId(OptionIdentifier.create(JEMLuxium.MOD_ID, "enabled", Boolean.class))
                        .setName(Component.translatable("jem_luxium.options.enabled"))
                        .setTooltip(Component.translatable("jem_luxium.options.enabled.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((state, enabled) -> state.enabled = enabled, state -> state.enabled)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(Boolean.TYPE, actionStorage)
                        .setId(OptionIdentifier.create(JEMLuxium.MOD_ID, "settings", Boolean.class))
                        .setName(Component.translatable("jem_luxium.options.settings"))
                        .setTooltip(Component.translatable("jem_luxium.options.settings.tooltip"))
                        .setControl(option -> new LuxiumActionControl(
                                option,
                                Component.translatable("jem_luxium.options.settings.open"),
                                LuxiumOptionPage::openLuxiumSettings
                        ))
                        .setBinding((state, value) -> state.value = value, state -> state.value)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());
    }

    private static void openLuxiumSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        minecraft.setScreen(new LuxiumConfigScreen(parent));
    }

    private static final class ActionOptionStorage implements me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage<ActionOptionStorage.State> {
        private final State state = new State();

        @Override
        public State getData() {
            return state;
        }

        @Override
        public void save() {
        }

        private static final class State {
            private boolean value;
        }
    }
}
