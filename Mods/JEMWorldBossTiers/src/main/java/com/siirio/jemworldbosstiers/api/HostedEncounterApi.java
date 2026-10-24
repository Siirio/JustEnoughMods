package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.encounter.HostedEncounters;
import com.siirio.jemworldbosstiers.encounter.HostedRaidScaling;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class HostedEncounterApi {
    /** Canonical immutable Start token, absent until the hosted encounter actually starts. */
    public static java.util.UUID attemptId(LivingEntity boss) {
        return com.siirio.jemworldbosstiers.encounter.HostedEncounters.attemptId(boss);
    }

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
            if (entity instanceof net.minecraft.world.entity.player.Player) return entity;
            net.minecraft.world.entity.Entity next=entity instanceof net.minecraft.world.entity.projectile.Projectile p ? p.getOwner()
                    : entity instanceof net.minecraft.world.entity.OwnableEntity owned ? owned.getOwner()
                    : entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt ? tnt.getOwner()
                    : entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud ? cloud.getOwner()
                    : entity instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs ? fangs.getOwner() : null;
            if (next == null && entity instanceof net.minecraft.world.entity.OwnableEntity owned && owned.getOwnerUUID() != null) return entity;
            if (next == null) next = com.siirio.jemworldbosstiers.encounter.HostedAllies.owner(entity);
            if (next == null) next = com.siirio.jemworldbosstiers.encounter.NativeEncounterSource.owner(entity);
            if(next==null) return entity;
            entity=next;
        }
        return entity;
    }

    /** A native tick scope is used only when the damage source has no explicit entity. */
    public static net.minecraft.world.entity.Entity damageOwner(net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() != null) return damageOwner(source.getEntity());
        if (source.getDirectEntity() != null) return damageOwner(source.getDirectEntity());
        return com.siirio.jemworldbosstiers.encounter.NativeEncounterSource.current();
    }

    public static boolean enabled(net.minecraft.server.MinecraftServer server) {
        return server != null && server.isDedicatedServer();
    }

    public static long disconnectGraceMillis() {
        return com.siirio.jemworldbosstiers.encounter.HostedConfig.GRACE.get() * 50L;
    }

    private static boolean raidEnabled(net.minecraft.server.MinecraftServer server) {
        return enabled(server) && net.minecraftforge.fml.ModList.get().isLoaded("jem_server");
    }

    public static boolean canHost(LivingEntity boss) {
        return WorldTierApi.profile(boss).filter(com.siirio.jemworldbosstiers.balance.BossProfile::hostedEncounter).isPresent()
                && !com.siirio.jemworldbosstiers.encounter.HostedGroup.member(boss);
    }

    /** Shared admission for control, support and damage; this does not bypass native immunity. */
    public static boolean canInteract(LivingEntity actor, LivingEntity target) {
        if (!enabled(target.getServer())) return true;
        if (!HostedEncounters.canInteract(actor, target)) return false;
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_server")) return true;
        try {
            return (boolean) Class.forName("com.siirio.jemserver.smp.events.EventSession")
                    .getMethod("canInteract", LivingEntity.class, LivingEntity.class).invoke(null, actor, target);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Installed event interaction contract failed", failure);
        }
    }

    public static boolean hold(LivingEntity boss) { return com.siirio.jemworldbosstiers.encounter.HostedLobby.hold(boss); }
    public static boolean held(LivingEntity boss) { return com.siirio.jemworldbosstiers.encounter.HostedLobby.held(boss); }
    public static boolean participating(ServerPlayer player) { return HostedEncounters.participating(player); }
    public static void release(LivingEntity boss) { com.siirio.jemworldbosstiers.encounter.HostedLobby.release(boss); }

    public static boolean prepareRaid(LivingEntity boss) {
        if (com.siirio.jemworldbosstiers.revival.NativeBossStorage.pending(boss)) return false;
        if (!raidEnabled(boss.getServer())) return false;
        if(preservesRaidBoss(boss)) {
            if(boss.level().isClientSide||!boss.isAlive()||HostedEncounters.isHosted(boss)||!isRaid(boss)) return false;
            var profile=WorldTierApi.profile(boss).orElse(null);
            if(profile==null) return false;
            HostedRaidScaling.restore(boss);
            return hold(boss);
        }
        if(!canPrepareRaid(boss)) return false;
        var profile=WorldTierApi.profile(boss).orElseThrow();
        var data=boss.getPersistentData();
        if(data.contains(com.siirio.jemworldbosstiers.encounter.EncounterData.ROOT_KEY))
            data.put(ORIGINAL_RAID_ENCOUNTER,data.getCompound(com.siirio.jemworldbosstiers.encounter.EncounterData.ROOT_KEY).copy());
        EncounterScaler.ensureScaled(boss,profile);
        com.siirio.jemworldbosstiers.encounter.EncounterData.convertToRaid(data);
        data.putBoolean(PRESERVE_RAID_BOSS,true);
        HostedRaidScaling.restore(boss);
        hold(boss);
        return true;
    }

    public static boolean canPrepareRaid(LivingEntity boss) {
        if (com.siirio.jemworldbosstiers.revival.NativeBossStorage.pending(boss)) return false;
        if(!raidEnabled(boss.getServer())||boss.level().isClientSide||!boss.isAlive()||HostedEncounters.isHosted(boss)||boss.getPersistentData().getBoolean(PRESERVE_RAID_BOSS)) return false;
        var profile=WorldTierApi.profile(boss).orElse(null);
        return profile!=null&&WorldTierData.get(boss.getServer()).defeatedBosses().contains(profile.key())
                &&RaidArenaApi.compatible(boss);
    }

    public static boolean preservesRaidBoss(LivingEntity boss) {
        return boss.getPersistentData().getBoolean(PRESERVE_RAID_BOSS);
    }

    public static boolean wasNativeBeforeRaid(LivingEntity boss) {
        var data=boss.getPersistentData();
        return data.contains(ORIGINAL_RAID_ENCOUNTER,net.minecraft.nbt.Tag.TAG_COMPOUND)
                &&!EncounterProvenance.RAID_EVENT.name().equals(data.getCompound(ORIGINAL_RAID_ENCOUNTER).getString("Provenance"));
    }

    public static void restorePreparedRaid(LivingEntity boss) {
        var data=boss.getPersistentData();
        HostedRaidScaling.restore(boss);
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
        return start(boss, agreedPlayers, raid, bounds, null, attempt -> {});
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreedPlayers, boolean raid,
                                net.minecraft.world.level.levelgen.structure.BoundingBox bounds, UUID partyId,
                                java.util.function.Consumer<UUID> beforeRelease) {
        if (com.siirio.jemworldbosstiers.revival.NativeBossStorage.pending(boss)) return false;
        if (!enabled(boss.getServer()) || raid && !raidEnabled(boss.getServer()) || boss.level().isClientSide || !boss.isAlive() || !canHost(boss) || HostedEncounters.isHosted(boss)) {
            return false;
        }
        var profile = WorldTierApi.profile(boss).orElse(null);
        if (profile == null || raid != isRaid(boss) || raid && !WorldTierData.get(boss.getServer()).defeatedBosses().contains(profile.key())) {
            return false;
        }
        if (agreedPlayers.isEmpty()) return false;
        for (UUID id : agreedPlayers) {
            ServerPlayer player = boss.getServer().getPlayerList().getPlayer(id);
            if (player == null || com.siirio.jemworldbosstiers.encounter.CampaignAdmission.blocked(player, boss) != null) return false;
        }
        EncounterScaler.ensureScaled(boss, profile);
        var encounter = WorldTierApi.encounter(boss).orElse(null);
        String spawnType = boss.getPersistentData().getString("forge:spawn_type");
        if (!spawnType.equals("SPAWN_EGG") && !spawnType.equals("COMMAND") && encounter != null && encounter.arenaId() != null
                && WorldTierData.get(boss.getServer()).liveBoss(encounter.arenaId()).filter(id -> !id.equals(boss.getUUID())).isPresent()) return false;
        return HostedEncounters.start(boss, agreedPlayers, raid, profile.arenaRadius(), bounds, partyId, beforeRelease);
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

    /** Called only after a native source-attributed heal has actually increased health. */
    public static void supportHealing(net.minecraft.world.entity.Entity healer, LivingEntity target, float actualHealing) {
        HostedEncounters.supportHealing(healer, target, actualHealing);
    }

    public static java.util.Set<UUID> eligibleParticipants(LivingEntity boss) {
        return HostedEncounters.eligible(boss);
    }
}
