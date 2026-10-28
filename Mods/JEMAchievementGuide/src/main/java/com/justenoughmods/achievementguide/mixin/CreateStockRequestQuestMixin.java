package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateOrderProofs;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.stockTicker.PackageOrderRequestPacket", remap = false)
public abstract class CreateStockRequestQuestMixin {
    @Redirect(method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/logistics/stockTicker/StockTickerBlockEntity;)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/stockTicker/StockTickerBlockEntity;broadcastPackageRequest(Lcom/simibubi/create/content/logistics/packagerLink/LogisticallyLinkedBehaviour$RequestType;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Lcom/simibubi/create/content/logistics/packager/IdentifiedInventory;Ljava/lang/String;)Z"), remap = false)
    private boolean jem$nativeRequest(StockTickerBlockEntity ticker, LogisticallyLinkedBehaviour.RequestType type,
                                      PackageOrderWithCrafts order, IdentifiedInventory ignored, String address,
                                      ServerPlayer actor, StockTickerBlockEntity original) {
        return CreateOrderProofs.request(actor, order, () -> ticker.broadcastPackageRequest(type, order, ignored, address));
    }
}
