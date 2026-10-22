package com.siirio.jemworldbosstiers.balance;

import com.siirio.jemworldbosstiers.compat.epicfight.ProgressionMelee;
import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment;
import com.siirio.jemworldbosstiers.enchantment.TierEnchantments;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModList;

import java.util.ArrayDeque;
import java.util.Deque;

/** Progression follows the physical action and the weapon used by that action. */
public final class ProgressionWeapons {
    private static final String SNAPSHOT = "JEMProgressionLaunch";
    private static final ResourceKey<DamageType> CURSED_ARROW_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("cataclysm", "maledictio_sagitta"));
    private static final ResourceKey<DamageType> SHREDDER_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("cataclysm", "shredder"));
    private static final ResourceLocation MEAT_SHREDDER = new ResourceLocation("cataclysm", "meat_shredder");
    private static final ThreadLocal<Deque<MeleeHit>> MELEE = new ThreadLocal<>();
    private static final ThreadLocal<Deque<Resolution>> RESOLUTIONS = new ThreadLocal<>();

    private ProgressionWeapons() { }

    public static boolean enchantedWeapon(ItemStack stack) {
        return ProgressionEnchantment.isWeaponEligible(stack)
                && EnchantmentHelper.getItemEnchantmentLevel(TierEnchantments.PROGRESSION.get(), stack) > 0;
    }

    public static boolean nativeMeleeHit(Entity target, DamageSource source, float amount, ItemStack weapon) {
        Deque<MeleeHit> hits = MELEE.get();
        if (hits == null) {
            hits = new ArrayDeque<>();
            MELEE.set(hits);
        }
        MeleeHit hit = new MeleeHit(target, source, weapon.copy());
        hits.push(hit);
        try {
            return target instanceof LivingEntity living
                    ? NativeBossDamage.physicalHit(living, source, amount, hit) : target.hurt(source, amount);
        } finally {
            hits.pop();
            if (hits.isEmpty()) MELEE.remove();
        }
    }

    /** Zero means this hit has no supported enchanted weapon origin. */
    public static int attackTier(LivingEntity target, DamageSource source, ServerPlayer player) {
        if (source.getDirectEntity() instanceof AbstractArrow arrow && (source.is(DamageTypes.ARROW)
                || source.is(CURSED_ARROW_DAMAGE) && BuiltInRegistries.ENTITY_TYPE.getKey(arrow.getType())
                .equals(new ResourceLocation("cataclysm", "phantom_arrow")))) {
            CompoundTag launch = arrow.getPersistentData().getCompound(SNAPSHOT);
            return launch.getBoolean("enchanted") ? launch.getInt("tier") : 0;
        }
        if (source.getDirectEntity() != player) return 0;
        ItemStack weapon = ItemStack.EMPTY;
        Deque<Resolution> resolutions = RESOLUTIONS.get();
        MeleeHit nativeHit = resolutions == null || resolutions.isEmpty() ? null : resolutions.peek().nativeHit;
        if (nativeHit != null && nativeHit.target == target && nativeHit.source == source
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(SHREDDER_DAMAGE)
                && MEAT_SHREDDER.equals(BuiltInRegistries.ITEM.getKey(nativeHit.weapon.getItem())))) {
            weapon = nativeHit.weapon;
        } else if (source.is(DamageTypes.PLAYER_ATTACK) && ModList.get().isLoaded("epicfight")) {
            weapon = ProgressionMelee.usedWeapon(source);
        }
        return enchantedWeapon(weapon) ? WorldTierData.get(player.server).tier() : 0;
    }

    public static void beginResolution(LivingEntity target, DamageSource source) {
        Deque<MeleeHit> hits = MELEE.get();
        MeleeHit nativeHit = hits == null ? null : hits.peek();
        if (nativeHit != null && !nativeHit.bound && nativeHit.target == target && nativeHit.source == source
                && NativeBossDamage.invocationIs(nativeHit)) {
            nativeHit.bound = true;
        } else {
            nativeHit = null;
        }
        Deque<Resolution> resolutions = RESOLUTIONS.get();
        if (resolutions == null) {
            resolutions = new ArrayDeque<>();
            RESOLUTIONS.set(resolutions);
        }
        resolutions.push(new Resolution(nativeHit));
    }

    public static void endResolution() {
        Deque<Resolution> resolutions = RESOLUTIONS.get();
        resolutions.pop();
        if (resolutions.isEmpty()) RESOLUTIONS.remove();
    }

    public static void captureLaunch(AbstractArrow arrow, ItemStack weapon, LivingEntity shooter) {
        if (arrow.level().isClientSide() || arrow.getPersistentData().contains(SNAPSHOT)) return;
        CompoundTag launch = new CompoundTag();
        boolean enchanted = shooter instanceof ServerPlayer && weapon.getItem() instanceof ProjectileWeaponItem
                && enchantedWeapon(weapon);
        int tier = shooter.getServer() == null ? 1 : WorldTierData.get(shooter.getServer()).tier();
        launch.putBoolean("enchanted", enchanted);
        launch.putInt("tier", tier);
        launch.put("weapon", weapon.copy().save(new CompoundTag()));
        arrow.getPersistentData().put(SNAPSHOT, launch);
        if (enchanted) {
            int piercing = tier >= 3 ? 3 : tier >= 2 ? 2 : 0;
            if (piercing > arrow.getPierceLevel()) arrow.setPierceLevel((byte) piercing);
        }
    }

    /** Unknown/custom launchers never infer a weapon from the owner's later hands. */
    public static void freezeUncaptured(AbstractArrow arrow) {
        if (!arrow.getPersistentData().contains(SNAPSHOT)) {
            arrow.getPersistentData().put(SNAPSHOT, new CompoundTag());
        }
    }

    private record Resolution(MeleeHit nativeHit) { }

    private static final class MeleeHit {
        final Entity target;
        final DamageSource source;
        final ItemStack weapon;
        boolean bound;

        MeleeHit(Entity target, DamageSource source, ItemStack weapon) {
            this.target = target;
            this.source = source;
            this.weapon = weapon;
        }
    }
}
