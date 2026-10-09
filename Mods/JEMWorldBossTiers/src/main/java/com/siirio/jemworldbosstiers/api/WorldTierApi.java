package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.encounter.EncounterData;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.encounter.SpawnDecision;
import com.siirio.jemworldbosstiers.encounter.SpawnProvenanceRules;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import com.siirio.jemworldbosstiers.revival.ArenaService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class WorldTierApi {
    private WorldTierApi() {
    }

    public static int currentTier(MinecraftServer server) {
        return WorldTierData.get(server).tier();
    }

    public static Optional<BossProfile> profile(LivingEntity entity) {
        return BalanceRegistry.boss(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static Optional<EncounterData> encounter(LivingEntity entity) {
        return EncounterData.read(entity.getPersistentData());
    }

    public static void markNativeEncounter(LivingEntity entity, String arenaId) {
        profile(entity).ifPresent(profile -> {
            EncounterScaler.initialize(entity, profile, EncounterProvenance.NATIVE, true, false, arenaId);
            EncounterData.verify(entity.getPersistentData(), EncounterProvenance.NATIVE, arenaId);
        });
    }

    public static void markStructureEncounter(LivingEntity entity, String arenaId) {
        profile(entity).ifPresent(profile -> {
            EncounterScaler.initialize(entity, profile, EncounterProvenance.STRUCTURE, true, false, arenaId);
            EncounterData.verify(entity.getPersistentData(), EncounterProvenance.STRUCTURE, arenaId);
        });
    }

    public static void markRevivalEncounter(LivingEntity entity, String arenaId) {
        profile(entity).ifPresent(profile -> EncounterScaler.initialize(entity, profile, EncounterProvenance.REVIVAL, false, true, arenaId));
    }

    public static void finalizeNativeSpawn(LivingEntity entity, String spawnType) {
        if (HostedEncounterApi.isRaid(entity)) {
            return;
        }
        profile(entity).ifPresent(profile -> {
            MinecraftServer server = ((net.minecraft.server.level.ServerLevel) entity.level()).getServer();
            WorldTierData data = WorldTierData.get(server);
            SpawnDecision decision = SpawnProvenanceRules.decide(spawnType, data.defeatedBosses().contains(profile.key()));
            if (!decision.progressionEligible() && !decision.rematch()) {
                return;
            }
            ArenaRecord arena = ArenaService.findOrRegister(entity, profile);
            EncounterProvenance provenance = decision.rematch() ? EncounterProvenance.REVIVAL : EncounterProvenance.NATIVE;
            EncounterScaler.initialize(entity, profile, provenance, decision.progressionEligible(), decision.rematch(), arena.id());
            if (decision.progressionEligible()) {
                EncounterData.verify(entity.getPersistentData(), provenance, arena.id());
            } else if (arena.unlocked() && arena.activeEncounterId() == null) {
                arena.activate(entity.getUUID(), ((net.minecraft.server.level.ServerLevel) entity.level()).getGameTime()).ifPresent(data::putArena);
            }
        });
    }
}
