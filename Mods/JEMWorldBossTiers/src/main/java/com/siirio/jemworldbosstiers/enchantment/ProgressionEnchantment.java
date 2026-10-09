package com.siirio.jemworldbosstiers.enchantment;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.ForgeRegistries;

public final class ProgressionEnchantment extends Enchantment {
    private static final TagKey<net.minecraft.world.item.Item> EXCLUDED_ITEMS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(JemWorldBossTiers.MOD_ID, "progression_excluded")
    );
    private static final TagKey<net.minecraft.world.item.Item> INCLUDED_WEAPONS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("jemcompat", "weapons")
    );
    private static final TagKey<net.minecraft.world.item.Item> NON_WEAPONS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("jemcompat", "not_weapons")
    );

    public ProgressionEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{
                EquipmentSlot.MAINHAND,
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        });
    }

    @Override
    public int getMinCost(int level) {
        return 20;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return isEligible(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return false;
    }

    private static boolean excluded(ItemStack stack) {
        return stack.is(EXCLUDED_ITEMS);
    }

    public static boolean isEligible(ItemStack stack) {
        if (excluded(stack) || stack.is(NON_WEAPONS)) {
            return false;
        }
        return isArmorEligible(stack)||isWeaponEligible(stack);
    }

    public static boolean isWeaponEligible(ItemStack stack) {
        if(excluded(stack)||stack.is(NON_WEAPONS)||stack.isEdible()) return false;
        if(stack.is(INCLUDED_WEAPONS)) return true;
        if(stack.getItem() instanceof net.minecraft.world.item.BlockItem) return false;
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        String path=id==null?"":id.getPath();
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof PickaxeItem
                || stack.getItem() instanceof ShovelItem
                || stack.getItem() instanceof HoeItem
                || stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof ShieldItem
                || hasMainHandDamage(stack)
                || hasCombatUse(stack)
                || containsAny(path,"sword","blade","spear","javelin","dagger","katana","scythe","sickle","claw","mace",
                "halberd","halbert","greatsword","battle_axe","shield","bow","crossbow","cannon");
    }

    private static boolean hasMainHandDamage(ItemStack stack) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)
                .stream()
                .anyMatch(modifier -> modifier.getAmount() > 0.0D);
    }

    private static boolean hasCombatUse(ItemStack stack) {
        UseAnim animation=stack.getUseAnimation();
        return animation==UseAnim.BOW||animation==UseAnim.CROSSBOW||animation==UseAnim.SPEAR;
    }

    private static boolean containsAny(String value,String... needles) {
        for(String needle:needles) if(value.contains(needle)) return true;
        return false;
    }

    public static boolean isArmorEligible(ItemStack stack) {
        return !excluded(stack) && stack.getItem() instanceof ArmorItem;
    }
}
