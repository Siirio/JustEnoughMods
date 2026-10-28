package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateParcelCarrier;
import com.justenoughmods.achievementguide.integration.create.CreateParcelProofs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class CreateParcelStackQuestMixin implements CreateParcelCarrier {
    @Unique private CompoundTag jem$parcel;
    @Override public CompoundTag jem$parcelProof() { return jem$parcel == null ? null : jem$parcel.copy(); }
    @Override public void jem$parcelProof(CompoundTag proof) { jem$parcel = proof == null ? null : proof.copy(); }

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void jem$nativeDiskRead(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(CreateParcelProofs.SAVE_KEY, 10)) {
            jem$parcel = tag.getCompound(CreateParcelProofs.SAVE_KEY).copy();
            if (CreateParcelProofs.proof((ItemStack)(Object)this) == null) jem$parcel = null;
        }
    }
    @Inject(method = "copy", at = @At("RETURN"))
    private void jem$nativeCopy(CallbackInfoReturnable<ItemStack> callback) {
        if (jem$parcel == null) return;
        CompoundTag proof = CreateParcelProofs.proof((ItemStack)(Object)this);
        if (proof != null) ((CreateParcelCarrier)(Object)callback.getReturnValue()).jem$parcelProof(proof);
    }
    @Inject(method = "save", at = @At("RETURN"))
    private void jem$nativeDiskWrite(CompoundTag target, CallbackInfoReturnable<CompoundTag> callback) {
        if (jem$parcel == null) return;
        CompoundTag proof = CreateParcelProofs.proof((ItemStack)(Object)this);
        if (proof != null) callback.getReturnValue().put(CreateParcelProofs.SAVE_KEY, proof);
    }
}
