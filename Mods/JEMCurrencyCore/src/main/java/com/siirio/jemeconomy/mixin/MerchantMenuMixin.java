package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.merchant.NativeMerchantAccess;
import com.siirio.jemeconomy.merchant.NativeTradeContainer;
import com.siirio.jemeconomy.merchant.NativeTradeSession;
import com.siirio.jemcurrency.CurrencyAuthority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenu implements NativeMerchantAccess {
    @Shadow @Final private MerchantContainer tradeContainer;
    @Unique private NativeTradeSession jem$session;
    protected MerchantMenuMixin(MenuType<?> type, int id) { super(type, id); }
    @Override public NativeTradeSession jem$session() { return jem$session; }
    @Override public MerchantContainer jem$container() { return tradeContainer; }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("TAIL"))
    private void jem$attach(int id, Inventory inventory, Merchant merchant, CallbackInfo callback) {
        if (!(inventory.player instanceof ServerPlayer player) || !(merchant instanceof AbstractVillager villager)
                || !CurrencyAuthority.active(player)) return;
        jem$session = new NativeTradeSession((MerchantMenu) (Object) this, tradeContainer, villager, player);
        ((NativeTradeContainer) tradeContainer).jem$bind(jem$session);
        Slot guardedResult = new com.siirio.jemeconomy.merchant.NativeCoinResultSlot(tradeContainer);
        // The original native result slot remains for real barter, including its callbacks.
        jem$session.keepNativeResult(slots.get(2), guardedResult);
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (jem$session != null && slot == 2 && !jem$session.nativeBarter()) {
            jem$session.rejectUnquotedTake(); sendAllDataToRemote(); return;
        }
        super.clicked(slot, button, type, player);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void jem$quickMove(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        if (jem$session != null && slot == 2 && !jem$session.nativeBarter()) {
            jem$session.rejectUnquotedTake(); callback.setReturnValue(ItemStack.EMPTY);
        }
    }
    @Inject(method = "setSelectionHint", at = @At("HEAD"), cancellable = true)
    private void jem$select(int index, CallbackInfo callback) {
        if (jem$session != null && !jem$session.select(index)) callback.cancel();
    }
    @Inject(method = "tryMoveItems", at = @At("HEAD"), cancellable = true)
    private void jem$autoFill(int index, CallbackInfo callback) {
        if (jem$session != null && !jem$session.nativeBarter()) {
            jem$session.autoFill(index); callback.cancel();
        }
    }
    @Inject(method = "removed", at = @At("HEAD"))
    private void jem$close(Player player, CallbackInfo callback) {
        if (jem$session != null) { jem$session.close(); ((NativeTradeContainer) tradeContainer).jem$bind(null); }
    }
    @Override public void broadcastChanges() {
        if (jem$session != null) jem$session.refresh();
        super.broadcastChanges();
    }
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void jem$distance(Player player, CallbackInfoReturnable<Boolean> callback) {
        if (jem$session != null && !jem$session.valid()) callback.setReturnValue(false);
    }
}
