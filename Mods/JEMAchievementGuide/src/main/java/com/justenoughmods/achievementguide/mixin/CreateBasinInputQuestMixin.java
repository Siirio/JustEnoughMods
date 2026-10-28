package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinBlock", remap = false)
public abstract class CreateBasinInputQuestMixin {
    @Redirect(method = "lambda$updateEntityAfterFallOn$3",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItem(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"),
            remap = false)
    private static ItemStack jem$supplied(IItemHandler handler, ItemStack input, boolean simulate,
                                          ItemEntity entity, BasinBlockEntity basin) {
        int before = input.getCount();
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, input, simulate);
        if (!simulate && remainder.getCount() < before) {
            CreateRunProofs.bind((BlockEntity)(Object)basin, basin.inputInventory, CreateRunProofs.actor(entity));
        }
        return remainder;
    }
}
