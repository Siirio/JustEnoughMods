package com.justenoughmods.achievementguide.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionInventoryPart$SlotDefinition", remap = false)
public interface CompressionDefinitionQuestAccess {
    @Accessor(value = "item", remap = false) ItemStack jem$nativeCompressionItem();
    @Accessor(value = "prevSlotMultiplier", remap = false) int jem$nativeCompressionMultiplier();
    @Accessor(value = "isAccessible", remap = false) boolean jem$nativeCompressionAccessible();
    @Accessor(value = "isCompressible", remap = false) boolean jem$nativeCompressionCompressible();
}
