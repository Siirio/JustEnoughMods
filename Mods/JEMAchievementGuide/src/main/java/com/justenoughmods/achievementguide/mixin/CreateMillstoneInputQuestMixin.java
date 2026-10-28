package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The exact native landing insertion, after accepted count is known. */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.millstone.MillstoneBlock", remap = false)
public abstract class CreateMillstoneInputQuestMixin {
    @Redirect(method = {"updateEntityAfterFallOn", "m_5548_"},
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"),
            remap = false)
    private ItemStack jem$supplied(IItemHandler handler, int slot, ItemStack input, boolean simulate,
                                   BlockGetter world, Entity entity) {
        int before = input.getCount();
        ItemStack remainder = handler.insertItem(slot, input, simulate);
        if (!simulate && remainder.getCount() < before && entity instanceof ItemEntity item) {
            // Iterate.hereAndBelow is the exact two-position lookup used by6.0.8.
            for (BlockPos position : new BlockPos[]{entity.blockPosition(), entity.blockPosition().below()}) {
                BlockEntity owner = world.getBlockEntity(position);
                if (owner instanceof MillstoneBlockEntity mill
                        && owner.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null) == handler) {
                    CreateRunProofs.bind(owner, mill.inputInv, CreateRunProofs.actor(item));
                    break;
                }
            }
        }
        return remainder;
    }
}
