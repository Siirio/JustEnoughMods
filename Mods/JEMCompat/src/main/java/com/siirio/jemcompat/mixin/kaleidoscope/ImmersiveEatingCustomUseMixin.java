package com.siirio.jemcompat.mixin.kaleidoscope;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.toame.food.network.ServerAnimationManager;
import org.toame.food.network.packet.CustomUsingPacket;
import org.toame.food.unique.UniqueItem;

/** Restricts client-selected child food to the server-owned active plate animation. */
@Mixin(value = CustomUsingPacket.class, remap = false)
public abstract class ImmersiveEatingCustomUseMixin {
    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true)
    private static void jem$validateCustomFood(
            NetworkEvent.Context context,
            CustomUsingPacket packet,
            CallbackInfo ci
    ) {
        ServerPlayer player = context.getSender();
        if (player == null || packet.hand() == null || packet.customItem() == null || packet.count() != 1) {
            ci.cancel();
            return;
        }

        ServerAnimationManager.AnimationState state = ServerAnimationManager.get(player);
        if (state == null || state.hand() != packet.hand() || !ServerAnimationManager.matchesHeldStack(player)) {
            ci.cancel();
            return;
        }

        ResourceLocation heldId = ForgeRegistries.ITEMS.getKey(player.getItemInHand(packet.hand()).getItem());
        ResourceLocation requestedId = ForgeRegistries.ITEMS.getKey(packet.customItem());
        if (heldId == null || requestedId == null || !requestedId.equals(UniqueItem.UNIQUE_ITEM_MAP.get(heldId))) {
            ci.cancel();
        }
    }

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
