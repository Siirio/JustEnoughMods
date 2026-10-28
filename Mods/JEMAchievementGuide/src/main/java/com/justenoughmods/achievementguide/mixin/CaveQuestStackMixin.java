package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.caves.CaveQuestStackCarrier;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestStackProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class CaveQuestStackMixin implements CaveQuestStackCarrier {
    @Unique private CompoundTag jem$cave;
    @Override public CompoundTag jem$caveReceipt() { return jem$cave == null ? new CompoundTag() : jem$cave.copy(); }
    @Override public void jem$caveReceipt(CompoundTag proof) { jem$cave = proof == null ? null : proof.copy(); }

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void jem$nativeDiskRead(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(CaveQuestStackProof.SAVE, 10)) {
            jem$cave = tag.getCompound(CaveQuestStackProof.SAVE).copy();
            if (CaveQuestStackProof.get((ItemStack)(Object)this).isEmpty()) jem$cave = null;
        }
    }
    @Inject(method = "copy", at = @At("RETURN"))
    private void jem$nativeCopy(CallbackInfoReturnable<ItemStack> callback) {
        if (jem$cave == null) return;
        CompoundTag proof = CaveQuestStackProof.get((ItemStack)(Object)this);
        if (!proof.isEmpty()) ((CaveQuestStackCarrier)(Object)callback.getReturnValue()).jem$caveReceipt(proof);
    }
    @Inject(method = "setCount", at = @At("HEAD"))
    private void jem$invalidateMutation(int count, CallbackInfo callback) { if (count != ((ItemStack)(Object)this).getCount()) jem$cave = null; }
    @Inject(method = "save", at = @At("RETURN"))
    private void jem$nativeDiskWrite(CompoundTag target, CallbackInfoReturnable<CompoundTag> callback) {
        if (jem$cave == null) return;
        CompoundTag proof = CaveQuestStackProof.get((ItemStack)(Object)this);
        if (!proof.isEmpty()) callback.getReturnValue().put(CaveQuestStackProof.SAVE, proof);
    }
}
