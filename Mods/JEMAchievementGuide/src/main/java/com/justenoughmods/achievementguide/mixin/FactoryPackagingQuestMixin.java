package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryIngredients;
import com.justenoughmods.achievementguide.integration.create.FactoryRecipeProof;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class FactoryPackagingQuestMixin {
    @Shadow public ItemStack heldBox;
    @Shadow public List<BigItemStack> queuedExitingPackages;
    @Unique private ItemStack jem$factoryBeforeBox;
    @Unique private int jem$factoryBeforeQueue;
    @Unique private boolean jem$factoryPacking;
    @Inject(method = "attemptToSend", at = @At("HEAD"))
    private void jem$beforeFactoryPacking(List<PackagingRequest> requests, CallbackInfo callback) {
        jem$factoryPacking = FactoryRecipeProof.active();
        if (jem$factoryPacking) { jem$factoryBeforeBox = heldBox; jem$factoryBeforeQueue = queuedExitingPackages.size(); }
    }
    @Inject(method = "attemptToSend", at = @At("RETURN"))
    private void jem$actualFactoryParcels(List<PackagingRequest> requests, CallbackInfo callback) {
        if (!jem$factoryPacking) return;
        if (heldBox != jem$factoryBeforeBox && !heldBox.isEmpty()) FactoryRecipeProof.parcel(heldBox);
        for (int i = jem$factoryBeforeQueue; i < queuedExitingPackages.size() && i - jem$factoryBeforeQueue < 256; i++) FactoryRecipeProof.parcel(queuedExitingPackages.get(i).stack);
        jem$factoryPacking = false; jem$factoryBeforeBox = null;
    }
    @Redirect(method = "unwrapBox", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/api/packager/unpacking/UnpackingHandler;unpack(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Ljava/util/List;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Z)Z"))
    private boolean jem$nativeFactoryIngredients(UnpackingHandler handler, Level level, BlockPos target, BlockState state, Direction side,
                                                List<ItemStack> items, PackageOrderWithCrafts order, boolean simulate, ItemStack box, boolean originalSimulation) {
        return FactoryIngredients.unpack((PackagerBlockEntity)(Object)this, box, simulate, target,
                () -> handler.unpack(level, target, state, side, items, order, simulate));
    }
}
