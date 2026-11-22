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
        return false;
    }

    /** Shared admission for control, support and damage; this does not bypass native immunity. */
    public static boolean canInteract(LivingEntity actor, LivingEntity target) {
        return true;
    }

    public static boolean hold(LivingEntity boss) { return false; }
    public static boolean held(LivingEntity boss) { return false; }
    public static boolean participating(ServerPlayer player) { return false; }
    public static void release(LivingEntity boss) { }

    public static boolean prepareRaid(LivingEntity boss) {
        return false;
    }

    public static boolean canPrepareRaid(LivingEntity boss) {
        return false;
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
        return false;
    }

    public static boolean isRaid(LivingEntity boss) {
        return WorldTierApi.encounter(boss).map(data -> data.provenance() == EncounterProvenance.RAID_EVENT).orElse(false);
    }

    public static List<ParticipantView> status(LivingEntity boss) {
        return List.of();
    }

    public static boolean cancel(LivingEntity boss) {
        return false;
    }

    public static boolean leave(ServerPlayer player) {
        return false;
    }

    public static boolean revive(ServerPlayer helper, UUID target) {
        return false;
    }

    public static void contribute(ServerPlayer player, double amount) {
        // Ordinary boss combat has no hosted contribution owner.
    }

    /** Called only after a native source-attributed heal has actually increased health. */
    public static void supportHealing(net.minecraft.world.entity.Entity healer, LivingEntity target, float actualHealing) {
        // Ordinary boss combat has no hosted support owner.
    }

    public static java.util.Set<UUID> eligibleParticipants(LivingEntity boss) {
        return java.util.Set.of();
    }
}
