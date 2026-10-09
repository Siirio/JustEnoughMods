package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.encounter.HostedEncounters;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class HostedEncounterApi {
    private static final String ORIGINAL_RAID_ENCOUNTER="jem:original_raid_encounter";
    private static final String PRESERVE_RAID_BOSS="jem:preserve_raid_boss";
    public record ParticipantView(UUID playerId, String state, boolean reviveAvailable, long remainingTicks,
                                  double contribution, double equipmentPower) {
    }

    private HostedEncounterApi() {
    }

    public static net.minecraft.world.entity.Entity damageOwner(net.minecraft.world.entity.Entity entity) {
        var seen=new java.util.HashSet<UUID>();
        for(int depth=0;entity!=null && depth<8 && seen.add(entity.getUUID());depth++) {
            net.minecraft.world.entity.Entity next=entity instanceof net.minecraft.world.entity.projectile.Projectile p ? p.getOwner()
                    : entity instanceof net.minecraft.world.entity.OwnableEntity owned ? owned.getOwner()
                    : entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt ? tnt.getOwner()
                    : entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud ? cloud.getOwner()
                    : entity instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs ? fangs.getOwner() : null;
            if(next==null) return entity;
            entity=next;
        }
        return entity;
    }

    public static boolean hold(LivingEntity boss) { return com.siirio.jemworldbosstiers.encounter.HostedLobby.hold(boss); }
    public static boolean held(LivingEntity boss) { return com.siirio.jemworldbosstiers.encounter.HostedLobby.held(boss); }
    public static void release(LivingEntity boss) { com.siirio.jemworldbosstiers.encounter.HostedLobby.release(boss); }

    public static boolean prepareRaid(LivingEntity boss) {
        if(!canPrepareRaid(boss)) return false;
        var profile=WorldTierApi.profile(boss).orElseThrow();
        var data=boss.getPersistentData();
        if(data.contains(com.siirio.jemworldbosstiers.encounter.EncounterData.ROOT_KEY))
            data.put(ORIGINAL_RAID_ENCOUNTER,data.getCompound(com.siirio.jemworldbosstiers.encounter.EncounterData.ROOT_KEY).copy());
        EncounterScaler.ensureScaled(boss,profile);
        com.siirio.jemworldbosstiers.encounter.EncounterData.convertToRaid(data);
        data.putBoolean(PRESERVE_RAID_BOSS,true);
        return true;
    }

    public static boolean canPrepareRaid(LivingEntity boss) {
        if(boss.level().isClientSide||!boss.isAlive()||HostedEncounters.isHosted(boss)||boss.getPersistentData().getBoolean(PRESERVE_RAID_BOSS)) return false;
        var profile=WorldTierApi.profile(boss).orElse(null);
        return profile!=null&&WorldTierData.get(boss.getServer()).defeatedBosses().contains(profile.key())
                &&RaidArenaApi.compatible(boss);
    }

    public static boolean preservesRaidBoss(LivingEntity boss) {
        return boss.getPersistentData().getBoolean(PRESERVE_RAID_BOSS);
    }

    public static void restorePreparedRaid(LivingEntity boss) {
        var data=boss.getPersistentData();
        if(data.contains(ORIGINAL_RAID_ENCOUNTER,net.minecraft.nbt.Tag.TAG_COMPOUND))
            data.put(com.siirio.jemworldbosstiers.encounter.EncounterData.ROOT_KEY,data.getCompound(ORIGINAL_RAID_ENCOUNTER).copy());
        else data.remove(com.siirio.jemworldbosstiers.encounter.EncounterData.ROOT_KEY);
        data.remove(ORIGINAL_RAID_ENCOUNTER);
        data.remove(PRESERVE_RAID_BOSS);
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreedPlayers, boolean raid) {
        return start(boss, agreedPlayers, raid, null);
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreedPlayers, boolean raid, net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        if (boss.level().isClientSide || !boss.isAlive() || HostedEncounters.isHosted(boss)) {
            return false;
        }
        var profile = WorldTierApi.profile(boss).orElse(null);
        if (profile == null || raid != isRaid(boss) || raid && !WorldTierData.get(boss.getServer()).defeatedBosses().contains(profile.key())) {
            return false;
        }
        EncounterScaler.ensureScaled(boss, profile);
        return HostedEncounters.start(boss, agreedPlayers, raid, profile.arenaRadius(), bounds);
    }

    public static boolean isRaid(LivingEntity boss) {
        return WorldTierApi.encounter(boss).map(data -> data.provenance() == EncounterProvenance.RAID_EVENT).orElse(false);
    }

    public static List<ParticipantView> status(LivingEntity boss) {
        return HostedEncounters.status(boss);
    }

    public static boolean cancel(LivingEntity boss) {
        return HostedEncounters.cancel(boss);
    }

    public static boolean leave(ServerPlayer player) {
        return HostedEncounters.leave(player);
    }

    public static boolean revive(ServerPlayer helper, UUID target) {
        return HostedEncounters.revive(helper, target);
    }

    public static void contribute(ServerPlayer player, double amount) {
        if (Double.isFinite(amount) && amount > 0) {
            HostedEncounters.contribute(player, amount);
        }
    }

    public static java.util.Set<UUID> eligibleParticipants(LivingEntity boss) {
        return HostedEncounters.eligible(boss);
    }
}
