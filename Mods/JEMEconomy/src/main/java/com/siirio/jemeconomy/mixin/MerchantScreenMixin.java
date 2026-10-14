package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.client.NativeTradeClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> implements com.siirio.jemeconomy.client.NativeTradeScreenAccess {
    @Shadow private int shopItem;
    @Shadow int scrollOff;
    @org.spongepowered.asm.mixin.Unique private int jem$revision = -1;
    @org.spongepowered.asm.mixin.Unique private int jem$serverSelection = -1;
    @Override public int jem$selected() { return shopItem; }
    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    /** Synchronizes authoritative selection without replacing or decorating the native screen. */
    @Inject(method = "render", at = @At("HEAD"))
    private void jem$selection(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        int count = menu.getOffers().size();
        var snapshot = NativeTradeClient.snapshot(menu);
        if (snapshot != null && snapshot.revision() != jem$revision) {
            if (snapshot.selected() != jem$serverSelection) shopItem = snapshot.selected();
            jem$serverSelection = snapshot.selected();
            jem$revision = snapshot.revision();
        }
        shopItem = Math.max(0, Math.min(shopItem, Math.max(0, count - 1)));
        scrollOff = Math.max(0, Math.min(scrollOff, Math.max(0, count - 7)));
    }
}
