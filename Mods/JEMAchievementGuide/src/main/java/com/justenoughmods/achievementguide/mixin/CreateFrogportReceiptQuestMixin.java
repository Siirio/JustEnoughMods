package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateParcelProofs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity", remap = false)
public abstract class CreateFrogportReceiptQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItem(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$nativeReceipt(IItemHandler inventory, ItemStack box, boolean simulate) {
        CompoundTag proof = simulate ? null : CreateParcelProofs.proof(box);
        int before = box.getCount();
        ItemStack remainder = ItemHandlerHelper.insertItem(inventory, box, simulate);
        if (!simulate && before == 1 && remainder.isEmpty()) CreateParcelProofs.received(proof, (BlockEntity)(Object)this);
        return remainder;
    }
}
