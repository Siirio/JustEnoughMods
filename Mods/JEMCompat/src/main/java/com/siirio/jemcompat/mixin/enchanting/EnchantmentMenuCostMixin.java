package com.siirio.jemcompat.mixin.enchanting;

import com.siirio.jemcompat.feature.enchanting.EnchantmentCostHistory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuCostMixin {
    @Shadow @Final private Container enchantSlots;
    @Unique private EnchantmentCostHistory.XpSnapshot jem$beforeXp;
    @Unique private ItemStack jem$beforeItem = ItemStack.EMPTY;

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void jem$beforeEnchant(Player player, int button, CallbackInfoReturnable<Boolean> callback) {
        jem$beforeXp = null;
        if (player instanceof ServerPlayer serverPlayer && player.containerMenu == (Object) this) {
            jem$beforeXp = EnchantmentCostHistory.xp(serverPlayer);
            jem$beforeItem = enchantSlots.getItem(0).copy();
        }
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void jem$afterEnchant(Player player, int button, CallbackInfoReturnable<Boolean> callback) {
        if (jem$beforeXp != null && callback.getReturnValue() && player instanceof ServerPlayer serverPlayer)
            EnchantmentCostHistory.record(jem$beforeItem, ItemStack.EMPTY, enchantSlots.getItem(0),
                    EnchantmentCostHistory.spent(jem$beforeXp, serverPlayer));
        jem$beforeXp = null;
        jem$beforeItem = ItemStack.EMPTY;
    }
}
