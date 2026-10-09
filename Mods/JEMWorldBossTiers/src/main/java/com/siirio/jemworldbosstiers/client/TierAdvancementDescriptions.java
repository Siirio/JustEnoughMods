package com.siirio.jemworldbosstiers.client;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.mixin.DisplayInfoAccessor;
import com.siirio.jemworldbosstiers.network.ClientTierState;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TierAdvancementDescriptions {
    private static final int TIER_COUNT = 5;

    private TierAdvancementDescriptions() {
    }

    public static void refresh(int defeatCount, int nextThreshold) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            Advancement advancement = Minecraft.getInstance().getConnection().getAdvancements().getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath(JemWorldBossTiers.MOD_ID, "world_tier/" + tier)
            );
            if (advancement == null) {
                continue;
            }
            DisplayInfo display = advancement.getDisplay();
            if (display == null) {
                continue;
            }
            String descriptionKey = "advancement.jem_world_boss_tiers.tier_" + tier + ".description";
            MutableComponent description = Component.translatable(descriptionKey).append(Component.literal(" "));
            if (nextThreshold > defeatCount) {
                description.append(Component.translatable("advancement.jem_world_boss_tiers.progress", defeatCount, nextThreshold));
            } else {
                description.append(Component.translatable("advancement.jem_world_boss_tiers.progress.max", defeatCount));
            }
            ((DisplayInfoAccessor) (Object) display).jemWorldBossTiers$setDescription(description);
        }
    }

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTierState.clear();
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        refresh(ClientTierState.defeatCount(), ClientTierState.nextThreshold());
    }
}
