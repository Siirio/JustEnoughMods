package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity", remap = false)
public abstract class CreateMillstoneQuestMixin {
    @Shadow public ItemStackHandler inputInv;
    @Shadow public ItemStackHandler outputInv;
    @Unique private UUID jem$actor;
    @Unique private int jem$inputCount;
    @Unique private int jem$outputCount;

    @Inject(method = "process", at = @At("HEAD"), remap = false)
    private void jem$begin(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        jem$actor = CreateRunProofs.pending(owner, inputInv);
        jem$inputCount = inputInv.getStackInSlot(0).getCount();
        jem$outputCount = CreateRunProofs.count(outputInv, "create:wheat_flour");
    }

    @Inject(method = "process", at = @At("RETURN"), remap = false)
    private void jem$committed(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (inputInv.getStackInSlot(0).getCount() < jem$inputCount) {
            CreateRunProofs.invalidate(owner);
            if (CreateRunProofs.count(outputInv, "create:wheat_flour") > jem$outputCount
                    && owner.getLevel() instanceof ServerLevel level) {
                CreateRunProofs.emit(level, jem$actor, "create_mill_flour");
            }
        }
        jem$actor = null;
    }
}
