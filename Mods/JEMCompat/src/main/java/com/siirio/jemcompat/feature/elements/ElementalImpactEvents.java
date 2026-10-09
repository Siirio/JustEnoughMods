package com.siirio.jemcompat.feature.elements;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ElementalImpactEvents {
    private static final TagKey<Item> FREEZING_ITEMS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(JEMCompat.MOD_ID, "freezing_impact_items")
    );
    private static final TagKey<Item> IGNITING_ITEMS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(JEMCompat.MOD_ID, "igniting_impact_items")
    );
    private static final String LAST_FREEZE_DAMAGE_TICK = "JEMLastFreezeDamageTick";
    private static final ResourceLocation COOL_DEATH_ADVANCEMENT = new ResourceLocation(
            "jem_guide",
            "cool_stuff/cool_death"
    );
    private static final int FREEZE_TICKS = 40;
    private static final int FREEZE_DAMAGE_COOLDOWN_TICKS = 200;
    private static final int SLOWNESS_TICKS = 100;
    private static final int SLOWNESS_AMPLIFIER = 1;
    private static final int FIRE_SECONDS = 5;
    private static final float FREEZE_DAMAGE = 1.0F;
    private static final int SNOWFLAKE_COUNT = 12;
    private static final double PARTICLE_WIDTH_SPREAD = 0.35;
    private static final double PARTICLE_HEIGHT_OFFSET = 0.5;
    private static final double PARTICLE_HEIGHT_SPREAD = 0.25;
    private static final double PARTICLE_SPEED = 0.02;

    private ElementalImpactEvents() {
    }

    @SubscribeEvent
    public static void meleeImpact(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker) {
            return;
        }
        apply(event.getEntity(), attacker.getMainHandItem());
    }

    @SubscribeEvent
    public static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || target.level().isClientSide()) {
            return;
        }
        Entity projectile = event.getProjectile();
        if (projectile instanceof Snowball snowball) {
            freeze(target, snowball.getOwner() instanceof ServerPlayer player ? player : null);
            return;
        }
        if (projectile instanceof ThrowableItemProjectile throwable) {
            apply(target, throwable.getItem());
        } else if (projectile.isOnFire()) {
            ignite(target);
        }
    }

    private static void apply(LivingEntity target, ItemStack impactItem) {
        if (impactItem.isEmpty()) {
            return;
        }
        if (impactItem.is(FREEZING_ITEMS)) {
            freeze(target, null);
        }
        if (target instanceof Mob && (impactItem.is(IGNITING_ITEMS) || isTorch(impactItem))) {
            ignite(target);
        }
    }

    private static boolean isTorch(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TorchBlock;
    }

    private static void freeze(LivingEntity target, ServerPlayer snowballOwner) {
        int frozenTicks = Math.max(
                SLOWNESS_TICKS,
                Math.min(target.getTicksRequiredToFreeze(), target.getTicksFrozen() + FREEZE_TICKS)
        );
        target.setTicksFrozen(frozenTicks);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, frozenTicks, SLOWNESS_AMPLIFIER));
        if (target.level() instanceof ServerLevel level) {
            long gameTime = level.getGameTime();
            long lastDamageTick = target.getPersistentData().getLong(LAST_FREEZE_DAMAGE_TICK);
            if (frozenTicks >= target.getTicksRequiredToFreeze()
                    && gameTime - lastDamageTick >= FREEZE_DAMAGE_COOLDOWN_TICKS) {
                boolean damaged = target.hurt(target.damageSources().freeze(), FREEZE_DAMAGE);
                target.getPersistentData().putLong(LAST_FREEZE_DAMAGE_TICK, gameTime);
                if (damaged && snowballOwner != null && target.isDeadOrDying()) {
                    com.siirio.jemcompat.advancement.AdvancementAwards.award(snowballOwner, COOL_DEATH_ADVANCEMENT);
                }
            }
            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    target.getX(),
                    target.getY() + target.getBbHeight() * PARTICLE_HEIGHT_OFFSET,
                    target.getZ(),
                    SNOWFLAKE_COUNT,
                    target.getBbWidth() * PARTICLE_WIDTH_SPREAD,
                    target.getBbHeight() * PARTICLE_HEIGHT_SPREAD,
                    target.getBbWidth() * PARTICLE_WIDTH_SPREAD,
                    PARTICLE_SPEED
            );
        }
    }

    private static void ignite(LivingEntity target) {
        if (!target.fireImmune()) {
            target.setSecondsOnFire(FIRE_SECONDS);
        }
    }
}
