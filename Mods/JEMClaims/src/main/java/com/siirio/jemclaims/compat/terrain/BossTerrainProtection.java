package com.siirio.jemclaims.compat.terrain;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_claims")
public final class BossTerrainProtection {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> ADDITIONAL_BOSSES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE, new net.minecraft.resources.ResourceLocation("jem_claims", "additional_bosses"));
    private BossTerrainProtection() {}

    public static boolean allows(BlockGetter level, BlockPos position) {
        return !(level instanceof ServerLevel serverLevel)
                || !FlanBridge.claimed(serverLevel, position);
    }

    public static Entity owner(Entity entity) {
        Set<Entity> visited = new HashSet<>();
        while (entity != null && visited.add(entity)) {
            Entity parent = entity instanceof Projectile projectile ? projectile.getOwner()
                    : entity instanceof OwnableEntity owned ? owned.getOwner() : null;
            if (parent == null) return entity;
            entity = parent;
        }
        return entity;
    }

    public static boolean isBoss(Entity entity) {
        return owner(entity) instanceof LivingEntity living && (WorldTierApi.profile(living).isPresent() || living.getType().is(ADDITIONAL_BOSSES));
    }

    private static boolean safe(Entity entity) {
        return entity.level() instanceof ServerLevel level && FlanBridge.claimed(level, entity.blockPosition())
                && !FlanBridge.environmental(level, entity.blockPosition(), ClaimPermission.HURTPLAYER);
    }

    private static boolean preventsCombat(LivingEntity victim, DamageSource source) {
        Entity attacker = owner(source.getEntity() == null ? source.getDirectEntity() : source.getEntity());
        return attacker != null && ((victim instanceof Player && isBoss(attacker)) || (attacker instanceof Player && isBoss(victim)))
                && (safe(victim) || safe(attacker));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (preventsCombat(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (preventsCombat(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void explode(ExplosionEvent.Detonate event) {
        if (isBoss(event.getExplosion().getIndirectSourceEntity()))
            event.getAffectedBlocks().removeIf(position -> !allows(event.getLevel(), position));
    }

    @SubscribeEvent
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (!isBoss(event.getEntity())) return;
        if (!allows(event.getLevel(), event.getPos()) || event instanceof BlockEvent.EntityMultiPlaceEvent multiple
                && multiple.getReplacedBlockSnapshots().stream().anyMatch(snapshot -> !allows(event.getLevel(), snapshot.getPos())))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void destroy(LivingDestroyBlockEvent event) {
        if (isBoss(event.getEntity()) && !allows(event.getEntity().level(), event.getPos())) event.setCanceled(true);
    }
}
