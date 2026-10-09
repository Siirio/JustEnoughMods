package com.siirio.jemworldbosstiers.balance;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class BossFrenzy {
    private static final TagKey<EntityType<?>> FORGE_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses"));
    private static final TagKey<EntityType<?>> COMMON_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("c", "bosses"));
    private static final TagKey<EntityType<?>> EXCLUDED_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(JemWorldBossTiers.MOD_ID, "boss_frenzy_excluded"));
    private static final UUID MOVEMENT_MODIFIER = modifierId("movement");
    private static final UUID ATTACK_MODIFIER = modifierId("attack");
    private static final String CRITICAL_KEY = "JEMBossFrenzyCritical";
    private static final int UPDATE_INTERVAL_TICKS = 4;
    private static final int HANDOFF_LIFETIME_TICKS = 100;
    private static final double HANDOFF_RADIUS = 16.0D;
    private static final double HANDOFF_RADIUS_SQUARED = HANDOFF_RADIUS * HANDOFF_RADIUS;
    private static final float CRITICAL_HEALTH_RATIO = 0.3F;
    private static final double CRITICAL_BONUS = 0.15D;
    private static final Map<MinecraftServer, ArrayDeque<Handoff>> HANDOFFS = new WeakHashMap<>();

    private BossFrenzy() {
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof LivingEntity boss && isBoss(boss)) {
            inheritCriticalStage(level, boss);
            update(boss);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity boss = event.getEntity();
        if (!boss.level().isClientSide() && boss.tickCount % UPDATE_INTERVAL_TICKS == 0 && isBoss(boss)) {
            update(boss);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        rememberCriticalStage(event.getEntity());
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss)) {
            return;
        }
        Entity.RemovalReason reason = boss.getRemovalReason();
        if (reason != null && reason != Entity.RemovalReason.UNLOADED_TO_CHUNK && reason != Entity.RemovalReason.CHANGED_DIMENSION) {
            rememberCriticalStage(boss);
        }
    }

    private static void update(LivingEntity boss) {
        float maximumHealth = boss.getMaxHealth();
        if (maximumHealth <= 0.0F) {
            return;
        }
        float healthRatio = boss.getHealth() / maximumHealth;
        if (healthRatio < CRITICAL_HEALTH_RATIO) {
            boss.getPersistentData().putBoolean(CRITICAL_KEY, true);
        }
        double bonus = boss.getPersistentData().getBoolean(CRITICAL_KEY) ? CRITICAL_BONUS : 0.0D;
        apply(boss.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_MODIFIER, "JEM boss frenzy movement", bonus);
        apply(boss.getAttribute(Attributes.ATTACK_SPEED), ATTACK_MODIFIER, "JEM boss frenzy attack", bonus);
    }

    private static void apply(AttributeInstance attribute, UUID modifierId, String name, double bonus) {
        if (attribute == null) {
            return;
        }
        AttributeModifier current = attribute.getModifier(modifierId);
        if (current != null && current.getAmount() == bonus) {
            return;
        }
        if (current != null) {
            attribute.removeModifier(modifierId);
        }
        if (bonus > 0.0D) {
            attribute.addPermanentModifier(new AttributeModifier(modifierId, name, bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static boolean isBoss(LivingEntity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return !entity.getPersistentData().getBoolean("jem:event_spawned") && !entity.getType().is(EXCLUDED_BOSSES) && (BalanceRegistry.boss(entityId).isPresent()
                || entity.getType().is(FORGE_BOSSES)
                || entity.getType().is(COMMON_BOSSES));
    }

    private static ResourceLocation identity(LivingEntity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return BalanceRegistry.boss(entityId).map(BossProfile::key).orElse(entityId);
    }

    private static void rememberCriticalStage(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !isBoss(entity) || !entity.getPersistentData().getBoolean(CRITICAL_KEY)) {
            return;
        }
        ArrayDeque<Handoff> handoffs = HANDOFFS.computeIfAbsent(level.getServer(), ignored -> new ArrayDeque<>());
        handoffs.removeIf(handoff -> handoff.source().equals(entity.getUUID()));
        handoffs.addLast(new Handoff(
                entity.getUUID(),
                level.dimension(),
                entity.position(),
                identity(entity),
                level.getGameTime() + HANDOFF_LIFETIME_TICKS
        ));
    }

    private static void inheritCriticalStage(ServerLevel level, LivingEntity entity) {
        ArrayDeque<Handoff> handoffs = HANDOFFS.get(level.getServer());
        if (handoffs == null) {
            return;
        }
        long gameTime = level.getGameTime();
        ResourceLocation identity = identity(entity);
        Iterator<Handoff> iterator = handoffs.iterator();
        while (iterator.hasNext()) {
            Handoff handoff = iterator.next();
            if (handoff.expiresAt() < gameTime) {
                iterator.remove();
                continue;
            }
            if (handoff.dimension().equals(level.dimension())
                    && handoff.identity().equals(identity)
                    && handoff.position().distanceToSqr(entity.position()) <= HANDOFF_RADIUS_SQUARED) {
                entity.getPersistentData().putBoolean(CRITICAL_KEY, true);
                iterator.remove();
                break;
            }
        }
        if (handoffs.isEmpty()) {
            HANDOFFS.remove(level.getServer());
        }
    }

    private static UUID modifierId(String attribute) {
        return UUID.nameUUIDFromBytes((JemWorldBossTiers.MOD_ID + ":boss-frenzy-" + attribute).getBytes(StandardCharsets.UTF_8));
    }

    private record Handoff(
            UUID source,
            ResourceKey<Level> dimension,
            Vec3 position,
            ResourceLocation identity,
            long expiresAt
    ) {
    }
}
