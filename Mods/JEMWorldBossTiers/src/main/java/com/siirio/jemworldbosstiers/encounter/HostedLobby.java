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
    private static final long TIMEOUT_TICKS = 18000;

    private HostedLobby() {}

    public static boolean hold(LivingEntity boss) {
        if (boss.level().isClientSide || !boss.isAlive() || HostedEncounters.isHosted(boss)
                || WorldTierApi.profile(boss).isEmpty()) return false;
        if (held(boss)) {
            boss.getPersistentData().getCompound(ROOT).putLong("Until", boss.level().getGameTime() + TIMEOUT_TICKS);
            return true;
        }
        CompoundTag state = new CompoundTag();
        state.putLong("Until", boss.level().getGameTime() + TIMEOUT_TICKS);
        boss.getPersistentData().put(ROOT, state);
        if (boss instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        BossPresentation.suppress(boss);
        return true;
    }

    public static boolean held(LivingEntity boss) { return boss.getPersistentData().contains(ROOT); }

    public static void release(LivingEntity boss) {
        if (boss.level().isClientSide || !held(boss)) return;
        if (boss instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(false);
        BossPresentation.restore(boss);
        boss.getPersistentData().remove(ROOT);
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity boss && held(boss)) BossPresentation.suppress(boss);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loaded(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity boss) release(boss);
    }
}
