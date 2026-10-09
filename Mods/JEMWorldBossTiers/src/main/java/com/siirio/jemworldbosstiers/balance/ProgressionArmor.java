package com.siirio.jemworldbosstiers.balance;

import com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment;
import com.siirio.jemworldbosstiers.enchantment.TierEnchantments;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ProgressionArmor {
    private static final float HEAVY_HIT_DAMAGE = 8.0F;
    private static final float SECONDARY_REDUCTION_SHARE = 0.25F;
    private static final float TIER_TWO_PROJECTILE_REDUCTION = 0.15F;
    private static final float TIER_THREE_PROJECTILE_REDUCTION = 0.25F;
    private static final float TIER_TWO_HEAVY_REDUCTION = 0.10F;
    private static final float TIER_THREE_HEAVY_REDUCTION = 0.15F;
    private static final float TIER_THREE_GUARD_REDUCTION = 0.35F;
    private static final float TIER_TWO_FALL_REDUCTION = 0.35F;
    private static final float TIER_THREE_FALL_REDUCTION = 0.60F;
    private static final float REFLECTION_CHANCE = 0.20F;
    private static final float REFLECTION_DAMAGE_SHARE = 0.50F;
    private static final float MAX_REFLECTION_DAMAGE = 4.0F;
    private static final long GUARD_COOLDOWN_TICKS = 240L;
    private static final int MOVEMENT_DURATION_TICKS = 60;
    private static final int LANDING_MOVEMENT_DURATION_TICKS = 40;
    private static final double ATTACK_SPEED_BONUS = 0.15D;
    private static final String GUARD_READY_AT = "JEMProgressionGuardReadyAt";
    private static final String ATTACK_SPEED_END = "JEMProgressionAttackSpeedEnd";
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("fb8f7250-1860-45ab-8423-15bf043f7357");

    private ProgressionArmor() {
    }

    public static void apply(LivingHurtEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!(wearer.level() instanceof ServerLevel level)) {
            return;
        }
        int tier = WorldTierData.get(level.getServer()).tier();
        if (tier < 2) {
            return;
        }
        List<Float> reductions = new ArrayList<>();
        boolean helmet = hasProgression(wearer.getItemBySlot(EquipmentSlot.HEAD));
        boolean chest = hasProgression(wearer.getItemBySlot(EquipmentSlot.CHEST));
        boolean legs = hasProgression(wearer.getItemBySlot(EquipmentSlot.LEGS));
        boolean boots = hasProgression(wearer.getItemBySlot(EquipmentSlot.FEET));
        boolean projectile = event.getSource().getDirectEntity() instanceof Projectile;
        boolean fall = event.getSource().is(DamageTypeTags.IS_FALL);

        if (helmet && projectile) {
            if (tier >= 3 && wearer.getRandom().nextFloat() < REFLECTION_CHANCE) {
                reflect(event, wearer, level);
                return;
            }
            reductions.add(tier >= 3 ? TIER_THREE_PROJECTILE_REDUCTION : TIER_TWO_PROJECTILE_REDUCTION);
        }
        if (chest && event.getAmount() >= HEAVY_HIT_DAMAGE) {
            reductions.add(tier >= 3 ? TIER_THREE_HEAVY_REDUCTION : TIER_TWO_HEAVY_REDUCTION);
        }
        if (chest && tier >= 3 && guardReady(wearer, level)) {
            reductions.add(TIER_THREE_GUARD_REDUCTION);
            wearer.getPersistentData().putLong(GUARD_READY_AT, level.getGameTime() + GUARD_COOLDOWN_TICKS);
            showGuard(wearer, level);
        }
        if (boots && fall) {
            reductions.add(tier >= 3 ? TIER_THREE_FALL_REDUCTION : TIER_TWO_FALL_REDUCTION);
        }
        event.setAmount(event.getAmount() * combinedMultiplier(reductions));
        if (legs) {
            applyLeggingsBoost(wearer, tier, level);
        }
        if (boots && fall && tier >= 3) {
            wearer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, LANDING_MOVEMENT_DURATION_TICKS, 1, false, true));
        }
    }

    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        AttributeInstance attackSpeed = event.player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null || attackSpeed.getModifier(ATTACK_SPEED_MODIFIER_ID) == null) {
            return;
        }
        if (event.player.level().getGameTime() >= event.player.getPersistentData().getLong(ATTACK_SPEED_END)) {
            attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
        }
    }

    private static boolean hasProgression(ItemStack stack) {
        return ProgressionEnchantment.isArmorEligible(stack)
                && EnchantmentHelper.getItemEnchantmentLevel(TierEnchantments.PROGRESSION.get(), stack) > 0;
    }

    private static float combinedMultiplier(List<Float> reductions) {
        if (reductions.isEmpty()) {
            return 1.0F;
        }
        reductions.sort(Comparator.reverseOrder());
        float multiplier = 1.0F - reductions.get(0);
        for (int index = 1; index < reductions.size(); index++) {
            multiplier *= 1.0F - reductions.get(index) * SECONDARY_REDUCTION_SHARE;
        }
        return multiplier;
    }

    private static boolean guardReady(LivingEntity wearer, ServerLevel level) {
        return level.getGameTime() >= wearer.getPersistentData().getLong(GUARD_READY_AT);
    }

    private static void applyLeggingsBoost(LivingEntity wearer, int tier, ServerLevel level) {
        wearer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MOVEMENT_DURATION_TICKS, tier >= 3 ? 1 : 0, false, true));
        if (tier < 3) {
            return;
        }
        AttributeInstance attackSpeed = wearer.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
            attackSpeed.addTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_MODIFIER_ID,
                    "JEM progression leggings",
                    ATTACK_SPEED_BONUS,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
            wearer.getPersistentData().putLong(ATTACK_SPEED_END, level.getGameTime() + MOVEMENT_DURATION_TICKS);
        }
    }

    private static void reflect(LivingHurtEvent event, LivingEntity wearer, ServerLevel level) {
        event.setCanceled(true);
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != wearer) {
            attacker.hurt(wearer.damageSources().thorns(wearer), Math.min(MAX_REFLECTION_DAMAGE, event.getAmount() * REFLECTION_DAMAGE_SHARE));
        }
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, wearer.getX(), wearer.getY() + wearer.getBbHeight() * 0.6D, wearer.getZ(), 18, wearer.getBbWidth() * 0.4D, wearer.getBbHeight() * 0.3D, wearer.getBbWidth() * 0.4D, 0.15D);
        level.playSound(null, wearer.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.25F);
    }

    private static void showGuard(LivingEntity wearer, ServerLevel level) {
        level.sendParticles(ParticleTypes.WAX_ON, wearer.getX(), wearer.getY() + wearer.getBbHeight() * 0.5D, wearer.getZ(), 14, wearer.getBbWidth() * 0.45D, wearer.getBbHeight() * 0.35D, wearer.getBbWidth() * 0.45D, 0.05D);
        level.playSound(null, wearer.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 0.7F, 0.8F);
    }
}
