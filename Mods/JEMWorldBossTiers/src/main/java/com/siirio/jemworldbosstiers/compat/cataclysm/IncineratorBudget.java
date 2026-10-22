package com.siirio.jemworldbosstiers.compat.cataclysm;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.UUID;

public final class IncineratorBudget {
    public static final String CAST = "JEMIncineratorCast";
    private static final String EXPIRES = "JEMIncineratorExpires";
    private static final String CASTS = "JEMIncineratorBudgets";
    private IncineratorBudget() { }

    public static void begin(LivingEntity caster) {
        if (!caster.level().isClientSide) caster.getPersistentData().putUUID(CAST, UUID.randomUUID());
    }

    public static void tag(Entity zone, LivingEntity caster) {
        if (caster.getPersistentData().hasUUID(CAST)) {
            zone.getPersistentData().putUUID(CAST, caster.getPersistentData().getUUID(CAST));
            // Native zones finish within 80 loaded ticks. An absolute 200-tick budget
            // lifetime prevents unloaded/reloaded zones from reopening an expired cast.
            zone.getPersistentData().putLong(EXPIRES, zone.level().getGameTime() + 200);
        }
    }

    public static boolean hurt(Entity zone, LivingEntity target, DamageSource source, float flat, float percent) {
        if (!zone.getPersistentData().hasUUID(CAST)
                || BalanceRegistry.boss(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())).isEmpty()) {
            return target.hurt(source, flat + target.getMaxHealth() * percent);
        }
        long expires = zone.getPersistentData().getLong(EXPIRES);
        if (expires <= target.level().getGameTime()) return target.hurt(source, flat);
        clean(target);
        CompoundTag budgets = target.getPersistentData().getCompound(CASTS);
        String key = zone.getPersistentData().getUUID(CAST).toString();
        CompoundTag budget = budgets.getCompound(key);
        float used = budget.getFloat("Used");
        float admitted = Math.max(0, Math.min(percent, 0.08F - used));
        // Reserve before hurt so a nested secondary hit cannot spend the same remainder.
        budget.putFloat("Used", used + admitted);
        budget.putLong("Expires", expires);
        budgets.put(key, budget);
        target.getPersistentData().put(CASTS, budgets);
        boolean accepted = false;
        float healthBefore = Math.max(0, target.getHealth());
        float absorptionBefore = Math.max(0, target.getAbsorptionAmount());
        boolean consumed = false;
        try {
            accepted = target.hurt(source, flat + target.getMaxHealth() * admitted);
            // Forge may return true even when a later damage event canceled the hit.
            consumed = accepted && (target.getHealth() < healthBefore
                    || target.getAbsorptionAmount() < absorptionBefore);
            return accepted;
        } finally {
            if (!consumed) {
                CompoundTag current = target.getPersistentData().getCompound(CASTS);
                CompoundTag entry = current.getCompound(key);
                entry.putFloat("Used", Math.max(0, entry.getFloat("Used") - admitted));
                current.put(key, entry);
                target.getPersistentData().put(CASTS, current);
            }
        }
    }

    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity().tickCount % 20 == 0) clean(event.getEntity());
    }

    private static void clean(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (!data.contains(CASTS)) return;
        CompoundTag budgets = data.getCompound(CASTS);
        long now = target.level().getGameTime();
        for (String key : new ArrayList<>(budgets.getAllKeys())) {
            if (budgets.getCompound(key).getLong("Expires") <= now) budgets.remove(key);
        }
        if (budgets.isEmpty()) data.remove(CASTS); else data.put(CASTS, budgets);
    }
}
