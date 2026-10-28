package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.CoopFoodProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.farm_and_charm.core.block.entity.ChickenCoopBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChickenCoopBlockEntity.class, remap = false)
public abstract class ChickenCoopProduceQuestMixin {
    @Redirect(method = "addChicken", at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/block/entity/ChickenCoopBlockEntity;addEgg()V"))
    private void jem$nativeProduced(ChickenCoopBlockEntity owner, Chicken chicken) {
        int before = owner.getEggCount(), known = CoopFoodProof.known(owner);
        owner.addEgg();
        if (chicken.isRemoved() && owner.containsChicken(chicken) && owner.getEggCount() == before + 1)
            CoopFoodProof.certify(owner, known + 1);
        else if (owner.getEggCount() == before && known > 0) CoopFoodProof.certify(owner, known);
    }
    @Inject(method = {"addEgg", "clearEggs"}, at = @At("HEAD"))
    private void jem$mutation(CallbackInfo callback) { CoopFoodProof.clear((BlockEntity)(Object)this); }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { CoopFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { CoopFoodProof.load((BlockEntity)(Object)this, tag); }
}
