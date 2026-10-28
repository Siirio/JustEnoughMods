package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateTransportInputs;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.belt.BeltBlock", remap = false)
public abstract class CreateTransportInputQuestMixin {
    @Redirect(method = "lambda$entityInside$0", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private static ItemStack jem$actualSupply(IItemHandler handler, int slot, ItemStack input, boolean simulate,
                                             ItemStack original, Entity entity, BeltBlockEntity belt) {
        if (!simulate && entity instanceof ItemEntity item && !item.level().isClientSide) {
            return CreateTransportInputs.insert(item, () -> handler.insertItem(slot, input, false));
        }
        return handler.insertItem(slot, input, simulate);
    }
}
