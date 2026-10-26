package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.client.NativeTradeClient;
import com.siirio.jemeconomy.merchant.NativeMerchantAccess;
import com.siirio.jemeconomy.merchant.NativeTradeQuote;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the server's quoted result visible while vanilla updates the payment inputs. */
@Mixin(MerchantContainer.class)
public abstract class ClientMerchantContainerMixin {
    @Shadow private int selectionHint;

    @Inject(method = "updateSellItem", at = @At("HEAD"), cancellable = true)
    private void jem$preserveServerQuote(CallbackInfo callback) {
        MerchantMenu menu = jem$menu();
        if (menu == null) return;
        NativeTradeQuote quote = NativeTradeClient.quote(menu, selectionHint);
        if (quote != null && quote.direction() != NativeTradeQuote.Direction.ITEM_BARTER) callback.cancel();
    }

    private MerchantMenu jem$menu() {
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof MerchantMenu menu)
                || ((NativeMerchantAccess) menu).jem$container() != (Object) this
                || NativeTradeClient.snapshot(menu) == null) return null;
        return menu;
    }
}
