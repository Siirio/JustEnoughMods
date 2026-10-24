package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class HostedLobby {
    private static final String ROOT = "JEMHostedLobby";

    private HostedLobby() {}

    public static boolean hold(LivingEntity boss) {
        var encounter = boss.getPersistentData().getCompound("JEMHostedEncounter");
        boolean terminal = encounter.getBoolean("FailureClosing") || encounter.getBoolean("LinkPending") || encounter.contains("VictoryAt");
        if (!HostedEncounterApi.enabled(boss.getServer()) || boss.level().isClientSide || !boss.isAlive() || HostedEncounters.isHosted(boss) && !retiringRaid(boss) && !terminal
                || !HostedEncounterApi.canHost(boss) && !HostedGroup.member(boss)) return false;
        if (held(boss)) {
            freeze(boss);
            return true;
        }
        CompoundTag state = new CompoundTag();
        if (boss instanceof net.minecraft.world.entity.Mob mob) state.putBoolean("NativeNoAi", mob.isNoAi());
        boss.getPersistentData().put(ROOT, state);
        freeze(boss);
        if (!HostedGroup.member(boss)) HostedGroup.stage(boss);
        return true;
    }

    private static void freeze(LivingEntity boss) {
        if (boss instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        boss.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        BossPresentation.suppress(boss);
    }

    public static boolean held(LivingEntity boss) {
        return com.siirio.jemworldbosstiers.revival.NativeBossStorage.pending(boss)
                || HostedEncounterApi.enabled(boss.getServer()) && boss.getPersistentData().contains(ROOT);
    }

    private static boolean retiringRaid(LivingEntity boss) {
        return boss.getServer() != null && WorldTierApi.encounter(boss)
                .filter(encounter -> encounter.arenaId() != null)
                .map(encounter -> com.siirio.jemworldbosstiers.progression.WorldTierData.get(boss.getServer()).raidReplacementPending(encounter.arenaId()))
                .orElse(false);
    }

    public static void release(LivingEntity boss) {
        if (boss.level().isClientSide || !boss.getPersistentData().contains(ROOT)) return;
        if (boss instanceof net.minecraft.world.entity.Mob mob)
            mob.setNoAi(boss.getPersistentData().getCompound(ROOT).getBoolean("NativeNoAi"));
        BossPresentation.restore(boss);
        boss.getPersistentData().remove(ROOT);
        if (!HostedGroup.member(boss)) HostedGroup.release(boss);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        var attacker = HostedEncounterApi.damageOwner(event.getSource().getEntity());
        if (attacker == null) attacker = HostedEncounterApi.damageOwner(event.getSource().getDirectEntity());
        if (held(event.getEntity()) || attacker instanceof LivingEntity boss && held(boss)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void targeting(LivingChangeTargetEvent event) {
        if (!event.getEntity().level().isClientSide && held(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide && held(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void damage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && held(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && held(event.getEntity())) {
            event.setCanceled(true);
            event.getEntity().setHealth(event.getEntity().getMaxHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity boss && held(boss)) BossPresentation.suppress(boss);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loaded(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity boss) {
            if (!HostedEncounterApi.enabled(boss.getServer())) {
                HostedEncounters.restoreNative(boss);
                return;
            }
            if (HostedEncounterApi.canHost(boss) && !HostedEncounters.isHosted(boss)) {
                hold(boss);
                return;
            }
            if (!held(boss)) return;
            if (!HostedEncounterApi.canHost(boss) && !HostedGroup.member(boss)
                    || HostedEncounters.isHosted(boss) && !retiringRaid(boss)) release(boss);
            else freeze(boss);
        }
    }
}
