package com.justenoughmods.achievementguide.mixin;

import com.google.common.collect.Multimap;
import com.justenoughmods.achievementguide.integration.create.FactoryRecipeProof;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LogisticsManager.class, remap = false)
public abstract class FactoryRequestDiscoveryQuestMixin {
    @Inject(method = "findPackagersForRequest", at = @At("RETURN"))
    private static void jem$exactNativeOrder(UUID network, PackageOrderWithCrafts order, IdentifiedInventory ignored, String address,
                                            CallbackInfoReturnable<Multimap<PackagerBlockEntity, PackagingRequest>> callback) {
        FactoryRecipeProof.discovered(order, callback.getReturnValue());
    }
}
