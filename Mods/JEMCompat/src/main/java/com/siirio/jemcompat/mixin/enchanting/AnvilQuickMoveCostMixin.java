package com.siirio.jemcompat.mixin.enchanting;

import com.siirio.jemcompat.feature.enchanting.EnchantmentCostHistory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shift-click copies the output before onTake; attach its provenance first. */
@Mixin(ItemCombinerMenu.class)
public abstract class AnvilQuickMoveCostMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void jem$beforeQuickMove(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> callback) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !((Object) this instanceof AnvilMenu menu)
                || player.containerMenu != (Object) this
                || slotIndex != menu.getResultSlot()) return;
        ItemStack result = menu.getSlot(slotIndex).getItem();
        if (!result.isEmpty())
            EnchantmentCostHistory.record(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), result,
                    EnchantmentCostHistory.paidForLevels(serverPlayer, menu.getCost()));
    }
}
