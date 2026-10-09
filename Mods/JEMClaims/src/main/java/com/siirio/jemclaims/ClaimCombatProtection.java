package com.siirio.jemclaims;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_claims")
public final class ClaimCombatProtection {
    private ClaimCombatProtection() {}

    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        Entity source = event.getSource().getEntity();
        if (source instanceof Projectile projectile) source = projectile.getOwner();
        if (source instanceof OwnableEntity owned) source = owned.getOwner();
        if (!(source instanceof ServerPlayer player) || player.getUUID().equals(target.getUUID())) return;
        if (!FlanBridge.can(player, target.serverLevel(), target.blockPosition(), ClaimPermission.HURTPLAYER)
                || !FlanBridge.can(player, player.serverLevel(), player.blockPosition(), ClaimPermission.HURTPLAYER)) event.setCanceled(true);
    }
}
