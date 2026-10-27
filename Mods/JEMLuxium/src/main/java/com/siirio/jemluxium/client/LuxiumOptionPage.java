package com.siirio.jemluxium.client;

import com.google.common.collect.ImmutableList;
import com.siirio.jemluxium.JEMLuxium;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        return ImmutableList.of(OptionGroup.createBuilder()
                .setId(new ResourceLocation(JEMLuxium.MOD_ID, "shader_selection"))
                .add(OptionImpl.createBuilder(Boolean.TYPE, storage)
                        .setId(OptionIdentifier.create(JEMLuxium.MOD_ID, "enabled", Boolean.class))
                        .setName(Component.translatable("jem_luxium.options.enabled"))
                        .setTooltip(Component.translatable("jem_luxium.options.enabled.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((state, enabled) -> state.enabled = enabled, state -> state.enabled)
                        .setEnabledPredicate(LuxiumShaderController::isLuxiumInstalled)
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .build());
    }
}
