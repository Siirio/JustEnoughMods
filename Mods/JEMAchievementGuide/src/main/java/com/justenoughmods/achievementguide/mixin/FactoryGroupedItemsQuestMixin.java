package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCargoTransfers;
import com.justenoughmods.achievementguide.integration.create.FactoryGridReceipt;
import com.justenoughmods.achievementguide.integration.create.FactoryResultReceipt;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import java.util.HashMap;
import java.util.Map;
import net.createmod.catnip.math.Pointing;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecipeGridHandler.GroupedItems.class, remap = false)
public abstract class FactoryGroupedItemsQuestMixin implements FactoryGridReceipt {
    @Shadow Map<Pair<Integer, Integer>, ItemStack> grid;
    @Unique private CompoundTag jem$factory = new CompoundTag();
    @Unique private CompoundTag jem$factoryMerge = new CompoundTag();
    @Unique private CompoundTag jem$factorySnapshot() {
        CompoundTag result = new CompoundTag();
        if (grid.size() > 1024) return result;
        grid.forEach((coordinate, stack) -> result.put(coordinate.getLeft() + "/" + coordinate.getRight(), stack.save(new CompoundTag())));
        return result;
    }
    @Override public CompoundTag jem$factoryReceipt() {
        return grid.size() <= 1024 && jem$factory.contains("origin", 10)
                && jem$factorySnapshot().equals(jem$factory.getCompound("grid")) ? jem$factory.copy() : new CompoundTag();
    }
    @Override public void jem$factoryReceipt(CompoundTag proof) {
        jem$factory = proof.copy();
        if (!proof.isEmpty() && grid.size() <= 1024) jem$factory.put("grid", jem$factorySnapshot());
        else jem$factory = new CompoundTag();
    }
    @Override public Map<String, Long> jem$factoryContents() {
        Map<String, Long> counts = new HashMap<>();
        if (grid.size() <= 1024) grid.values().stream().filter(stack -> !stack.isEmpty())
                .forEach(stack -> counts.merge(CreateCargoTransfers.fingerprint(stack), (long)stack.getCount(), Long::sum));
        return counts;
    }
    @Override public boolean jem$factoryHasItems() { return grid.size() > 1024 || grid.values().stream().anyMatch(stack -> !stack.isEmpty()); }

    @Inject(method = "<init>(Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void jem$nativeProductGroup(ItemStack result, CallbackInfo callback) {
        CompoundTag product = ((FactoryResultReceipt)(Object)result).jem$factoryResult();
        if (!product.isEmpty()) {
            jem$factoryReceipt(product);
            ((FactoryResultReceipt)(Object)result).jem$factoryResult(new CompoundTag());
        }
    }
    @Inject(method = "mergeOnto", at = @At("HEAD"))
    private void jem$beforeFactoryMerge(RecipeGridHandler.GroupedItems target, Pointing direction, CallbackInfo callback) {
        jem$factoryMerge = new CompoundTag();
        FactoryGridReceipt destination = (FactoryGridReceipt)(Object)target;
        CompoundTag source = jem$factoryReceipt(), dest = destination.jem$factoryReceipt();
        boolean sourceItems = jem$factoryHasItems(), destItems = destination.jem$factoryHasItems();
        if (!sourceItems) jem$factoryMerge = dest;
        else if (!destItems) jem$factoryMerge = source;
        else if (!source.isEmpty() && !dest.isEmpty() && source.getCompound("origin").equals(dest.getCompound("origin"))
                && source.getString("phase").equals("ingredients") && dest.getString("phase").equals("ingredients")
                && source.getBoolean("confirmed") && dest.getBoolean("confirmed")) jem$factoryMerge = dest;
        else if (!dest.isEmpty() && dest.getString("phase").equals("product")) {
            ItemStack product = ItemStack.of(dest.getCompound("origin").getCompound("product"));
            if (!jem$factoryContents().containsKey(CreateCargoTransfers.fingerprint(product))) jem$factoryMerge = dest;
        }
    }
    @Inject(method = "mergeOnto", at = @At("RETURN"))
    private void jem$afterFactoryMerge(RecipeGridHandler.GroupedItems target, Pointing direction, CallbackInfo callback) {
        ((FactoryGridReceipt)(Object)target).jem$factoryReceipt(jem$factoryMerge);
        jem$factoryMerge = new CompoundTag();
    }
    @Inject(method = "write", at = @At("TAIL"))
    private void jem$writeFactoryJob(CompoundTag tag, CallbackInfo callback) {
        CompoundTag proof = jem$factoryReceipt();
        if (!proof.isEmpty()) tag.put("jemFactoryGroupedV4", proof);
    }
    @Inject(method = "read", at = @At("RETURN"))
    private static void jem$readFactoryJob(CompoundTag tag, CallbackInfoReturnable<RecipeGridHandler.GroupedItems> callback) {
        FactoryGridReceipt group = (FactoryGridReceipt)(Object)callback.getReturnValue();
        CompoundTag proof = tag.getCompound("jemFactoryGroupedV4");
        // Validate saved exact grid before assigning a fresh observational snapshot.
        if (!proof.contains("grid", 10) || tag.getList("Grid", 10).size() > 1024) return;
        CompoundTag actual = new CompoundTag();
        for (var raw : tag.getList("Grid", 10)) {
            CompoundTag cell = (CompoundTag)raw;
            actual.put(cell.getInt("x") + "/" + cell.getInt("y"), cell.getCompound("item").copy());
        }
        if (actual.equals(proof.getCompound("grid"))) group.jem$factoryReceipt(proof);
    }
}
