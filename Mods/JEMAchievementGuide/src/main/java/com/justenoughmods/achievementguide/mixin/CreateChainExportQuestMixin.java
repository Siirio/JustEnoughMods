package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateParcelProofs;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.packagePort.PackagePortTarget$ChainConveyorFrogportTarget", remap = false)
public abstract class CreateChainExportQuestMixin {
    @Redirect(method = "export", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;addLoopingPackage(Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorPackage;)Z"), remap = false)
    private boolean jem$nativeLoopExport(ChainConveyorBlockEntity conveyor, ChainConveyorPackage parcel,
                                         LevelAccessor world, BlockPos sender, ItemStack box, boolean simulate) {
        boolean accepted = conveyor.addLoopingPackage(parcel);
        if (accepted && !simulate) CreateParcelProofs.exported(parcel.item, world, sender);
        return accepted;
    }
    @Redirect(method = "export", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;addTravellingPackage(Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorPackage;Lnet/minecraft/core/BlockPos;)Z"), remap = false)
    private boolean jem$nativeTravellingExport(ChainConveyorBlockEntity conveyor, ChainConveyorPackage parcel, BlockPos connection,
                                               LevelAccessor world, BlockPos sender, ItemStack box, boolean simulate) {
        boolean accepted = conveyor.addTravellingPackage(parcel, connection);
        if (accepted && !simulate) CreateParcelProofs.exported(parcel.item, world, sender);
        return accepted;
    }
}
