package com.siirio.jemcompat.client;

import com.siirio.jemcompat.JEMCompat;
import com.siirio.jemcompat.config.JEMClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SourceNameTooltipFilter {
    private SourceNameTooltipFilter() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void filter(ItemTooltipEvent event) {
        if (JEMClientConfig.SHOW_MOD_NAMES.get()) {
            return;
        }
        event.getToolTip().removeIf(line -> ModNames.ALL.contains(
                ChatFormatting.stripFormatting(line.getString()).trim()));
    }

    private static final class ModNames {
        private static final Set<String> ALL = ModList.get().getMods().stream()
                .map(mod -> mod.getDisplayName().trim())
                .collect(Collectors.toUnmodifiableSet());
    }
}
