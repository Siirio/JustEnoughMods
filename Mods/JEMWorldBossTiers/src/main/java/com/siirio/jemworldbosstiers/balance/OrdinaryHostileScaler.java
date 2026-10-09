package com.siirio.jemworldbosstiers.balance;

import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

import java.util.UUID;

public final class OrdinaryHostileScaler {
    private static final UUID HEALTH_MODIFIER = UUID.nameUUIDFromBytes("jem-world-boss-tiers:ordinary-hostile-health".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final UUID DAMAGE_MODIFIER = UUID.nameUUIDFromBytes("jem-world-boss-tiers:ordinary-hostile-damage".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final TagKey<EntityType<?>> FORGE_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses"));
    private static final TagKey<EntityType<?>> COMMON_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("c", "bosses"));

    private OrdinaryHostileScaler() {
    }

    public static void apply(LivingEntity entity, MinecraftServer server) {
        if (!ordinaryHostile(entity)) {
            remove(entity);
            return;
        }
        double multiplier = multiplier(WorldTierData.get(server).tier());
        updateHealth(entity, multiplier);
        updateAttribute(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER, "JEM ordinary hostile damage", multiplier);
    }

    public static void reconcile(MinecraftServer server) {
        server.getAllLevels().forEach(level -> level.getAllEntities().forEach(entity -> {
            if (entity instanceof LivingEntity living) {
                apply(living, server);
            }
        }));
    }

    private static boolean ordinaryHostile(LivingEntity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entity instanceof Enemy
                && !entity.getPersistentData().contains("jem:event_session_id")
                && BalanceRegistry.boss(entityId).isEmpty()
                && !entity.getType().is(FORGE_BOSSES)
                && !entity.getType().is(COMMON_BOSSES);
    }

    private static void updateHealth(LivingEntity entity, double multiplier) {
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        double oldMaximum = health.getValue();
        double ratio = oldMaximum <= 0.0D ? 1.0D : entity.getHealth() / oldMaximum;
        updateAttribute(health, HEALTH_MODIFIER, "JEM ordinary hostile health", multiplier);
        entity.setHealth((float) Math.max(1.0D, health.getValue() * ratio));
    }

    private static void remove(LivingEntity entity) {
        removeModifier(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER);
        removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER);
    }

    private static void updateAttribute(AttributeInstance attribute, UUID id, String name, double multiplier) {
        if (attribute == null) {
            return;
        }
        removeModifier(attribute, id);
        if (multiplier > 0.0D) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, multiplier, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void removeModifier(AttributeInstance attribute, UUID id) {
        if (attribute != null && attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }

    private static double multiplier(int tier) {
        return switch (tier) {
            case 3 -> 0.10D;
            case 4 -> 0.15D;
            case 5 -> 0.20D;
            default -> 0.0D;
        };
    }
}
