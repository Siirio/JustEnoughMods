package com.siirio.jemworldbosstiers.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.siirio.jemworldbosstiers.enchantment.TierEnchantments;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public final class ProgressionBookLootModifier extends LootModifier {
    public static final Codec<ProgressionBookLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, ProgressionBookLootModifier::new));

    public ProgressionBookLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var enchantment = TierEnchantments.PROGRESSION.get();
        generatedLoot.add(EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(enchantment, enchantment.getMaxLevel())));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
