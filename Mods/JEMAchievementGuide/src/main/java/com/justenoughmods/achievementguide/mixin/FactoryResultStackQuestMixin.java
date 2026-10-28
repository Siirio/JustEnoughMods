package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryResultReceipt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public abstract class FactoryResultStackQuestMixin implements FactoryResultReceipt {
    @Unique private CompoundTag jem$factoryResult = new CompoundTag();
    @Override public CompoundTag jem$factoryResult() { return jem$factoryResult.copy(); }
    @Override public void jem$factoryResult(CompoundTag proof) { jem$factoryResult = proof.copy(); }
}
