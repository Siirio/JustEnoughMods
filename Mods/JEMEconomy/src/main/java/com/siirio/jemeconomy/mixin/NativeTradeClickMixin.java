package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.client.NativeTradeClient;
import com.siirio.jemeconomy.client.NativeTradeScreenAccess;
import com.siirio.jemeconomy.merchant.NativeTradeQuote;
import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.network.NativeTradeActionPacket;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class NativeTradeClickMixin {
    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void jem$quotedTake(int containerId, int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        if (slot != 2 || !(player.containerMenu instanceof MerchantMenu menu)
                || !(Minecraft.getInstance().screen instanceof NativeTradeScreenAccess screen)) return;
        var snapshot = NativeTradeClient.snapshot(menu); var quote = NativeTradeClient.quote(menu, screen.jem$selected());
        if (snapshot == null || quote == null || quote.direction() == NativeTradeQuote.Direction.ITEM_BARTER) return;
        EconomyNetwork.tradeAction(new NativeTradeActionPacket(containerId, snapshot.revision(), screen.jem$selected(), button, type, UUID.randomUUID()));
        callback.cancel();
    }
}
