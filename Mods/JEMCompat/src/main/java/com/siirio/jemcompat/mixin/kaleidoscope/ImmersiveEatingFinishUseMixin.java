package com.siirio.jemcompat.mixin.kaleidoscope;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.toame.food.network.packet.FinishUsePacket;

/** Restores the normal Forge completion event omitted by Immersive Eating. */
@Mixin(value = FinishUsePacket.class, remap = false)
public abstract class ImmersiveEatingFinishUseMixin {
    @Redirect(
            method = "lambda$handle$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
                    remap = true
            )
    )
    private static ItemStack jem$finishWithForgeEvent(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack consumed = stack.copy();
        ItemStack result = stack.finishUsingItem(level, entity);
        return ForgeEventFactory.onItemUseFinish(entity, consumed, 0, result);
    }
}
