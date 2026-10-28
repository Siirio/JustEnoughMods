package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.justenoughmods.achievementguide.integration.create.CreateTransportInputs;
import com.justenoughmods.achievementguide.integration.create.CreateTransportProof;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack", remap = false)
public abstract class CreateTransportProofQuestMixin implements CreateTransportProof {
    @Shadow public ItemStack stack;
    @Unique private CompoundTag jem$proof = new CompoundTag();
    @Unique private CompoundTag jem$advance = new CompoundTag();
    @Override public CompoundTag jem$transportProof() { return jem$supplier() == null ? new CompoundTag() : jem$proof.copy(); }
    @Override public void jem$transportProof(CompoundTag proof) {
        jem$proof = proof.copy();
        if (jem$supplier() == null) jem$proof = new CompoundTag();
    }
    @Override public UUID jem$supplier() {
        return !stack.isEmpty() && jem$proof.hasUUID("actor") && stack.save(new CompoundTag()).equals(jem$proof.getCompound("stack"))
                ? jem$proof.getUUID("actor") : null;
    }
    @Override public void jem$clearAdvance() { jem$advance = new CompoundTag(); }
    @Override public void jem$nativeAdvance(ItemStack output, String recipe, int step, boolean completed) {
        UUID actor = jem$supplier();
        if (actor == null) return;
        CompoundTag state = stack.hasTag() ? stack.getTag().getCompound("SequencedAssembly") : new CompoundTag();
        if (step == 0 ? !state.isEmpty() : !jem$proof.getBoolean("initialized")
                || !recipe.equals(jem$proof.getString("recipe")) || state.getInt("Step") != step) return;
        jem$advance = new CompoundTag();
        jem$advance.putUUID("actor", actor);
        jem$advance.put("stack", output.save(new CompoundTag()));
        jem$advance.putBoolean("initialized", !completed);
        jem$advance.putString("recipe", recipe);
        jem$advance.putBoolean("precision", completed && CreateRunProofs.item(output, "create:precision_mechanism"));
    }
    @Override public void jem$applyAdvance(CreateTransportProof output, ItemStack item) {
        if (item.save(new CompoundTag()).equals(jem$advance.getCompound("stack"))) output.jem$transportProof(jem$advance);
    }
    @Override public boolean jem$precisionCompleted() { return jem$supplier() != null && jem$proof.getBoolean("precision"); }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void jem$nativeInput(ItemStack item, CallbackInfo callback) { jem$proof = CreateTransportInputs.initial(item); }
    @Inject(method = "getSimilar", at = @At("RETURN"), remap = false)
    private void jem$copy(CallbackInfoReturnable<TransportedItemStack> callback) {
        ((CreateTransportProof)(Object)callback.getReturnValue()).jem$transportProof(jem$transportProof());
    }
    @Inject(method = "serializeNBT", at = @At("RETURN"), remap = false)
    private void jem$save(CallbackInfoReturnable<CompoundTag> callback) {
        CompoundTag proof = jem$transportProof();
        if (!proof.isEmpty()) callback.getReturnValue().put("jemQuestSupplierV1", proof);
    }
    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private static void jem$load(CompoundTag tag, CallbackInfoReturnable<TransportedItemStack> callback) {
        ((CreateTransportProof)(Object)callback.getReturnValue()).jem$transportProof(tag.getCompound("jemQuestSupplierV1"));
    }
}
