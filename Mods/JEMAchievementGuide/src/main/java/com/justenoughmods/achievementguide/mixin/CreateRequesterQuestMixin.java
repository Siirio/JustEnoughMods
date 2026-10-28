package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateOrderProofs;
import com.justenoughmods.achievementguide.integration.create.CreateRequesterProofs;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity", remap = false)
public abstract class CreateRequesterQuestMixin {
    @Unique private boolean jem$edge;
    @Redirect(method = "onRedstonePowerChanged", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/redstoneRequester/RedstoneRequesterBlockEntity;triggerRequest()V"), remap = false)
    private void jem$nativeRisingEdge(RedstoneRequesterBlockEntity owner) {
        jem$edge = true;
        try { owner.triggerRequest(); } finally { jem$edge = false; }
    }
    @Redirect(method = "triggerRequest", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/redstoneRequester/RedstoneRequesterBlockEntity;broadcastPackageRequest(Lcom/simibubi/create/content/logistics/packagerLink/LogisticallyLinkedBehaviour$RequestType;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Lcom/simibubi/create/content/logistics/packager/IdentifiedInventory;Ljava/lang/String;)Z"), remap = false)
    private boolean jem$fulfilled(RedstoneRequesterBlockEntity owner, LogisticallyLinkedBehaviour.RequestType type,
                                  PackageOrderWithCrafts order, IdentifiedInventory ignored, String address) {
        return CreateOrderProofs.request(jem$edge ? CreateRequesterProofs.actor(owner) : null, order,
                "create_redstone_order", null, () -> owner.broadcastPackageRequest(type, order, ignored, address));
    }
}
