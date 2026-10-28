package com.justenoughmods.achievementguide.integration.combattools;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "jem_advancements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatToolLifecycle {
    private CombatToolLifecycle() {}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { CombatCorpseProof.clear(); CombatWaterProof.clear(); CombatRuneProof.clear(); CombatCraftProof.clear(); CombatDropProof.clear(); CombatTentacleProof.clear(); CombatLifeProof.clear(); CombatRegrowthProof.clear(); if (net.minecraftforge.fml.ModList.get().isLoaded("guardvillagers")) CombatGuardProof.clear(); CombatBrewingProof.clear(); CombatCampfireProof.clear(); CombatEffectProof.clear(); CombatTreasureProof.clear(); CombatRuneControlProof.clear(); CombatDeferredProof.clear(); }
}
