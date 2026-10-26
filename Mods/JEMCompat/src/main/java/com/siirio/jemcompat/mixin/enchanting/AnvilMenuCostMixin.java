package com.siirio.jemcompat.mixin.enchanting;

import com.siirio.jemcompat.feature.enchanting.EnchantmentCostHistory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EasyAnvils' installed ModAnvilMenu calls the native AnvilMenu.onTake path. */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuCostMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$beforeAnvil(Player player, ItemStack result, CallbackInfo callback) {
        if (result.isEmpty() || !(player instanceof ServerPlayer serverPlayer)
                || player.containerMenu != (Object) this) return;
        AnvilMenu menu = (AnvilMenu) (Object) this;
        EnchantmentCostHistory.record(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), result,
                EnchantmentCostHistory.paidForLevels(serverPlayer, menu.getCost()));
    }
}
