package com.siirio.jemserver.smp;

import com.siirio.jemidentity.api.IdentityPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Dedicated-server welcome entitlement and authoritative authentication/race-choice damage guard. */
@Mod.EventBusSubscriber(modid = "jem_server")
public final class StarterWelcome {
    private StarterWelcome() {}

    private static boolean protectedPlayer(ServerPlayer player) {
        if (!SmpEnvironment.active(player)) return false;
        var principal = IdentityPolicy.resolve(player);
        return principal.isEmpty() || DomainFeatures.loaded("jem_roles") && RacesAdapter.unselected(player, principal.get().accountId());
    }

    private static final class RacesAdapter {
        static boolean unselected(ServerPlayer player, java.util.UUID account) { return com.siirio.jemroles.api.RaceApi.get(player.server, account).isEmpty(); }
    }
    private static net.minecraft.world.entity.Entity damageOwner(net.minecraft.world.entity.Entity entity) {
        return DomainFeatures.loaded("jem_world_boss_tiers") ? BossDamageAdapter.owner(entity) : entity;
    }
    private static final class BossDamageAdapter {
        static net.minecraft.world.entity.Entity owner(net.minecraft.world.entity.Entity entity) { return com.siirio.jemworldbosstiers.api.HostedEncounterApi.damageOwner(entity); }
    }

    private static boolean blocked(net.minecraft.world.entity.LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
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
        if (player.tickCount % 20 != 0 || !player.isAlive()) return;
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return;
        try { StarterKitDelivery.deliver(player, principal.get().accountId()); }
        catch (java.io.IOException | RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Starter kit delivery retained for account {}", principal.get().accountId(), failure);
            // Do not expose an inventory whose native receipt failed to persist to further interactions.
            player.connection.disconnect(net.minecraft.network.chat.Component.translatable("jem.starter.storage_unavailable"));
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
            var receipt = StarterKitDelivery.persisted(original).getCompound(StarterKitDelivery.RECEIPT);
            if (!receipt.isEmpty()) StarterKitDelivery.persisted(replacement).put(StarterKitDelivery.RECEIPT, receipt.copy());
        }
    }
}
