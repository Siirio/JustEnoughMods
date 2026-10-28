package com.justenoughmods.achievementguide.integration.magiclast;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "jem_advancements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MagicLastLifecycle {
    private MagicLastLifecycle() {}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { MagicLastOutputProof.clear(); if (net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) { MagicLastFocusProof.clear(); MagicLastCauldronProof.clear(); MagicLastBloodProof.clear(); MagicLastSpellProof.clear(); MagicLastWorldProof.clear(); } MagicLastAutoHealProof.clear(); }
}
