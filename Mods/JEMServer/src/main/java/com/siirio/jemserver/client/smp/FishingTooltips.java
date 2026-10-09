package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.fishing.FishingProfiles;
import com.li64.tide.data.journal.FishRarity;
import com.li64.tide.data.rods.CustomRodManager;
import com.li64.tide.registries.TideItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = Dist.CLIENT)
public final class FishingTooltips {
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!FishingProfiles.clientActive() || event.getEntity() == null || !FishingProfiles.isRod(event.getItemStack())) return;
        var profile = FishingProfiles.get(event.getItemStack());
        var access = Component.translatable("jem_server.fishing.overworld");
        if (profile.nether() || CustomRodManager.getHook(event.getItemStack()).is(TideItems.LAVAPROOF_HOOK)) access.append(", ").append(Component.translatable("jem_server.fishing.nether"));
        if (profile.end()) access.append(", ").append(Component.translatable("jem_server.fishing.end"));
        event.getToolTip().add(Component.translatable("jem_server.fishing.access", access).withStyle(ChatFormatting.GRAY));
        if (profile.intrinsicVoid()) event.getToolTip().add(Component.translatable("jem_server.fishing.intrinsic_void").withStyle(ChatFormatting.DARK_PURPLE));
        if (!Screen.hasShiftDown()) { event.getToolTip().add(Component.translatable("jem_server.fishing.shift").withStyle(ChatFormatting.DARK_GRAY)); return; }
        double[] chances = FishingProfiles.chances(event.getItemStack());
        event.getToolTip().add(Component.translatable("jem_server.fishing.fixed_odds").withStyle(ChatFormatting.GOLD));
        for (FishRarity rarity : FishRarity.values()) event.getToolTip().add(Component.translatable("jem_server.fishing.chance",
                Component.translatable("jem_server.fishing.rarity_chance." + rarity.name().toLowerCase(java.util.Locale.ROOT)),
                String.format(java.util.Locale.ROOT, "%.2f", chances[rarity.ordinal()])).withStyle(ChatFormatting.GRAY));
    }
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { FishingProfiles.clearClient(); }
    private FishingTooltips() {}
}
