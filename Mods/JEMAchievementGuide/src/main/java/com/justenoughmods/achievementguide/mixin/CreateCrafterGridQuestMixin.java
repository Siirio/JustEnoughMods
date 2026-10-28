package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCrafterGrid;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.math.Pointing;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.crafter.RecipeGridHandler$GroupedItems", remap = false)
public abstract class CreateCrafterGridQuestMixin implements CreateCrafterGrid {
    @Shadow Map<Pair<Integer, Integer>, ItemStack> grid;
    @Unique private CompoundTag jem$proof = new CompoundTag();
    @Unique private UUID jem$mergingActor;

    @Unique private CompoundTag jem$snapshot() {
        CompoundTag snapshot = new CompoundTag();
        for (Map.Entry<Pair<Integer, Integer>, ItemStack> entry : grid.entrySet()) {
            snapshot.put(entry.getKey().getLeft() + "," + entry.getKey().getRight(), entry.getValue().save(new CompoundTag()));
        }
        return snapshot;
    }
    @Override public boolean jem$bounded() { return grid.size() <= 1024; }
    @Override public boolean jem$hasIngredients() {
        return jem$bounded() && grid.values().stream().anyMatch(stack -> !stack.isEmpty());
    }
    @Override public UUID jem$supplier() {
        return jem$bounded() && jem$proof.hasUUID("actor") && jem$snapshot().equals(jem$proof.getCompound("grid"))
                ? jem$proof.getUUID("actor") : null;
    }
    @Override public void jem$supplier(UUID actor) {
        jem$proof = new CompoundTag();
        if (actor != null && jem$bounded() && jem$hasIngredients()) {
            jem$proof.putUUID("actor", actor);
            jem$proof.put("grid", jem$snapshot());
        }
    }
    @Override public void jem$restore(CompoundTag proof) {
        jem$proof = proof.copy();
        if (jem$supplier() == null) jem$proof = new CompoundTag();
    }
    @Override public boolean jem$containsOutput(ItemStack output) {
        ItemStack actual = grid.get(Pair.of(0, 0));
        return actual != null && !actual.isEmpty() && actual.save(new CompoundTag()).equals(output.save(new CompoundTag()));
    }
    @Inject(method = "mergeOnto", at = @At("HEAD"), remap = false)
    private void jem$beforeMerge(RecipeGridHandler.GroupedItems target, Pointing direction, CallbackInfo callback) {
        jem$mergingActor = null;
        CreateCrafterGrid destination = (CreateCrafterGrid)(Object)target;
        if (!jem$bounded() || !destination.jem$bounded()) return;
        boolean sourceMaterial = jem$hasIngredients(), targetMaterial = destination.jem$hasIngredients();
        UUID sourceActor = jem$supplier(), targetActor = destination.jem$supplier();
        if (sourceMaterial && targetMaterial) {
            // Every real ingredient group must retain the same authenticated supplier.
            if (sourceActor != null && sourceActor.equals(targetActor)) jem$mergingActor = sourceActor;
        } else if (sourceMaterial) jem$mergingActor = sourceActor;
        else if (targetMaterial) jem$mergingActor = targetActor;
    }
    @Inject(method = "mergeOnto", at = @At("RETURN"), remap = false)
    private void jem$merged(RecipeGridHandler.GroupedItems target, Pointing direction, CallbackInfo callback) {
        ((CreateCrafterGrid)(Object)target).jem$supplier(jem$mergingActor);
        jem$mergingActor = null;
    }
    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void jem$save(CompoundTag tag, CallbackInfo callback) {
        if (jem$supplier() != null) tag.put("jemQuestSupplierV1", jem$proof.copy());
    }
    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private static void jem$load(CompoundTag tag, CallbackInfoReturnable<RecipeGridHandler.GroupedItems> callback) {
        ((CreateCrafterGrid)(Object)callback.getReturnValue()).jem$restore(tag.getCompound("jemQuestSupplierV1"));
    }
}
