package com.siirio.jemworldbosstiers.balance;

import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.encounter.EncounterData;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.encounter.PendingHealthSync;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class EncounterScaler {
    private static final String HEALTH_SCALED_KEY = "HealthScaled";
    private static final String HEALTH_SYNC_PENDING_KEY = "HealthSyncPending";
    private static final String INITIAL_HEALTH_KEY = "InitialHealth";

    private EncounterScaler() {
    }

    public static int ensureScaled(LivingEntity entity, BossProfile profile) {
        return initialize(entity, profile, EncounterProvenance.UNVERIFIED, false, false, null);
    }

    public static int initialize(LivingEntity entity, BossProfile profile, EncounterProvenance provenance, boolean progressionEligible, boolean rematch, String arenaId) {
        int currentTier = entity.level() instanceof ServerLevel level ? WorldTierData.get(level.getServer()).tier() : 1;
        EncounterData encounter = EncounterData.create(entity.getPersistentData(), profile.key(), currentTier, provenance, progressionEligible, rematch, arenaId);
        if (encounter.provenance() == EncounterProvenance.UNVERIFIED && provenance != EncounterProvenance.UNVERIFIED) {
            encounter = EncounterData.classify(entity.getPersistentData(), provenance, progressionEligible, rematch, arenaId);
        }
        int tier = encounter.tier();
        CompoundTag data = entity.getPersistentData().getCompound(EncounterData.ROOT_KEY);
        if (!profile.scalesWithWorldTier()) {
            return tier;
        }
        if (data.getBoolean(HEALTH_SYNC_PENDING_KEY)) {
            PendingHealthSync.enqueue(entity);
            return tier;
        }
        float previousMaxHealth = entity.getMaxHealth();
        float healthRatio = previousMaxHealth == 0.0F ? 1.0F : entity.getHealth() / previousMaxHealth;
        profile.attributes().forEach((attributeId, values) -> apply(entity, attributeId, values.valueAt(tier)));
        if (profile.attributes().isEmpty()) {
            applyMultiplier(entity, new ResourceLocation("minecraft", "generic.max_health"), genericMultiplier(tier));
            applyMultiplier(entity, new ResourceLocation("minecraft", "generic.attack_damage"), genericMultiplier(tier));
        }
        applyTierHealthBoost(entity, tier);
        if (data.getBoolean(HEALTH_SCALED_KEY) && Float.compare(previousMaxHealth, entity.getMaxHealth()) != 0)
            entity.setHealth(entity.getMaxHealth() * healthRatio);
        if (!data.getBoolean(HEALTH_SCALED_KEY)) {
            float initialHealth = Math.max(1.0F, entity.getMaxHealth() * healthRatio);
            entity.setHealth(initialHealth);
            data.putFloat(INITIAL_HEALTH_KEY, initialHealth);
            data.putBoolean(HEALTH_SYNC_PENDING_KEY, true);
            entity.getPersistentData().put(EncounterData.ROOT_KEY, data);
            PendingHealthSync.enqueue(entity);
        }
        return tier;
    }

    public static boolean finishInitialHealthScaling(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData().getCompound(EncounterData.ROOT_KEY);
        if (!data.getBoolean(HEALTH_SYNC_PENDING_KEY)) {
            return true;
        }
        if (entity.tickCount == 0) {
            return false;
        }
        entity.setHealth(Math.min(entity.getMaxHealth(), Math.max(1.0F, data.getFloat(INITIAL_HEALTH_KEY))));
        data.remove(INITIAL_HEALTH_KEY);
        data.remove(HEALTH_SYNC_PENDING_KEY);
        data.putBoolean(HEALTH_SCALED_KEY, true);
        entity.getPersistentData().put(EncounterData.ROOT_KEY, data);
        return true;
    }

    public static double healthAbsorption(LivingEntity entity) {
        var health = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (health == null || entity.getMaxHealth() <= 0) return 1;
        return Math.max(1, effectiveHealth(health) / entity.getMaxHealth());
    }

    static double effectiveHealth(AttributeInstance health) {
        double base = health.getBaseValue();
        for (var modifier : health.getModifiers(AttributeModifier.Operation.ADDITION)) base += modifier.getAmount();
        double total = base;
        for (var modifier : health.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) total += base * modifier.getAmount();
        for (var modifier : health.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) total *= 1 + modifier.getAmount();
        return total;
    }

    public static int encounterTier(LivingEntity entity) {
        return EncounterData.read(entity.getPersistentData()).map(EncounterData::tier).orElse(1);
    }

    private static void apply(LivingEntity entity, ResourceLocation attributeId, double targetValue) {
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId);
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        UUID modifierId = UUID.nameUUIDFromBytes(("jem-world-boss-tiers:boss:" + attributeId).getBytes(StandardCharsets.UTF_8));
        double delta = targetValue - instance.getBaseValue();
        AttributeModifier current = instance.getModifier(modifierId);
        if (current != null && Double.compare(current.getAmount(), delta) == 0) {
            return;
        }
        if (current != null) {
            instance.removeModifier(modifierId);
        }
        if (Double.compare(delta, 0.0D) != 0) {
            instance.addPermanentModifier(new AttributeModifier(modifierId, "JEM boss tier", delta, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void applyMultiplier(LivingEntity entity, ResourceLocation attributeId, double multiplier) {
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId);
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        UUID modifierId = UUID.nameUUIDFromBytes(("jem-world-boss-tiers:generic:" + attributeId).getBytes(StandardCharsets.UTF_8));
        AttributeModifier current = instance.getModifier(modifierId);
        double amount = multiplier - 1.0D;
        if (current != null && Double.compare(current.getAmount(), amount) == 0) {
            return;
        }
        if (current != null) {
            instance.removeModifier(modifierId);
        }
        if (Double.compare(amount, 0.0D) != 0) {
            instance.addPermanentModifier(new AttributeModifier(modifierId, "JEM generic boss tier", amount, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void applyTierHealthBoost(LivingEntity entity, int tier) {
        var health = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (health == null) return;
        UUID id = UUID.nameUUIDFromBytes("jem-world-boss-tiers:health-boost".getBytes(StandardCharsets.UTF_8));
        double amount = BossTierScaling.boost(tier) - 1;
        var modifier = health.getModifier(id);
        if (modifier != null && Double.compare(modifier.getAmount(), amount) == 0) return;
        health.removeModifier(id);
        health.addPermanentModifier(new AttributeModifier(id, "JEM world tier health boost", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static double genericMultiplier(int tier) {
        return switch (tier) {
            case 1 -> 0.90D;
            case 2 -> 1.00D;
            case 3 -> 1.10D;
            case 4 -> 1.20D;
            default -> 1.30D;
        };
    }
}
