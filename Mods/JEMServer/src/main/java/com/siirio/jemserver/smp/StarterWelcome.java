package com.siirio.jemserver.smp;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Authoritative dedicated-server authentication and race-choice damage guard. */
@Mod.EventBusSubscriber(modid = "jem_server")
public final class StarterWelcome {
    private StarterWelcome() {}

    private static boolean protectedPlayer(ServerPlayer player) {
        if (!SmpEnvironment.active(player)) return false;
        var principal = IdentityPolicy.resolve(player);
        return principal.isEmpty() || DomainFeatures.loaded("jem_roles") && RacesAdapter.unselected(player, principal.get().accountId());
    }

    private static final class RacesAdapter {
        static boolean unselected(ServerPlayer player, UUID account) { return RaceApi.get(player.server, account).isEmpty(); }
    }
    private static Entity damageOwner(Entity entity) {
        return DomainFeatures.loaded("jem_world_boss_tiers") ? BossDamageAdapter.owner(entity) : entity;
    }
    private static final class BossDamageAdapter {
        static Entity owner(Entity entity) { return HostedEncounterApi.damageOwner(entity); }
    }

    private static boolean blocked(LivingEntity target, DamageSource source) {
        if (target instanceof ServerPlayer player && protectedPlayer(player)) return true;
        var owner = damageOwner(source.getEntity());
        if (owner == null) owner = damageOwner(source.getDirectEntity());
        return owner instanceof ServerPlayer player && protectedPlayer(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void attack(LivingAttackEvent event) {
        if (blocked(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void hurt(LivingHurtEvent event) {
        if (blocked(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void damage(LivingDamageEvent event) {
        if (blocked(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && protectedPlayer(player)) {
            event.setCanceled(true);
            player.setHealth(Math.max(1F, player.getHealth()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void melee(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && protectedPlayer(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !SmpEnvironment.active(player)) return;
        if (protectedPlayer(player)) {
            player.fallDistance = 0;
            player.clearFire();
            player.setAirSupply(player.getMaxAirSupply());
        }
    }
}
