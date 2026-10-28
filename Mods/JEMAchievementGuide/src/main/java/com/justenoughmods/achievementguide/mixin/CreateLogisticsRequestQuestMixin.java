package com.justenoughmods.achievementguide.mixin;

import com.google.common.collect.Multimap;
import com.justenoughmods.achievementguide.integration.create.CreateOrderProofs;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.packagerLink.LogisticsManager", remap = false)
public abstract class CreateLogisticsRequestQuestMixin {
    @Inject(method = "findPackagersForRequest", at = @At("RETURN"), remap = false)
    private static void jem$nativeOrderIdentities(UUID network, PackageOrderWithCrafts order, IdentifiedInventory ignored,
                                                 String address, CallbackInfoReturnable<Multimap<PackagerBlockEntity, PackagingRequest>> callback) {
        CreateOrderProofs.discovered(order, callback.getReturnValue().values());
    }
}
