package com.siirio.jemroles.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Legacy serializer ID remains readable; the finalized modifier is the sole active owner. */
public final class PiglinFortuneLoot extends LootModifier {
    public static final Codec<PiglinFortuneLoot> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, PiglinFortuneLoot::new));
    public PiglinFortuneLoot(LootItemCondition[] conditions) { super(conditions); }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        return loot;
    }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
