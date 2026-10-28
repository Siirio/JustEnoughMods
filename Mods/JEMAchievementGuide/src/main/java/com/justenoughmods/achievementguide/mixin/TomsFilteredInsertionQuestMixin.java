package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.TomsFilterEndpointAccess;
import com.justenoughmods.achievementguide.integration.TomsFilterProof;
import com.tom.storagemod.tile.FilteredInventoryCableConnectorBlockEntity;
import com.tom.storagemod.util.FilteredInventoryHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tom.storagemod.util.FilteredInventoryHandler", remap = false)
public abstract class TomsFilteredInsertionQuestMixin implements TomsFilterEndpointAccess {
    @Shadow private IItemHandler parent;
    @Shadow private boolean isInFilter(ItemStack item) { throw new AssertionError(); }
    @Unique private FilteredInventoryCableConnectorBlockEntity jem$filterEndpoint;
    @Unique private final List<TomsFilterProof.Insertion> jem$filterInsertions = new ArrayList<>();
    @Unique private int jem$suppressedFilterDepth;
    @Override public void jem$bindFilterEndpoint(FilteredInventoryCableConnectorBlockEntity endpoint) { jem$filterEndpoint = endpoint; }
    @Inject(method = "insertItem", at = @At("HEAD"))
    private void jem$beforeInsert(int slot, ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        if (jem$filterInsertions.size() >= 8) { jem$suppressedFilterDepth++; return; }
        jem$filterInsertions.add(TomsFilterProof.beforeInsert(jem$filterEndpoint, parent, item, simulate));
    }
    @Redirect(method = "insertItem", at = @At(value = "INVOKE", target =
            "Lcom/tom/storagemod/util/FilteredInventoryHandler;isInFilter(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$nativeDecision(FilteredInventoryHandler handler, ItemStack item) {
        boolean allowed = isInFilter(item);
        if (jem$suppressedFilterDepth == 0 && !jem$filterInsertions.isEmpty()) {
            TomsFilterProof.decision(jem$filterInsertions.get(jem$filterInsertions.size() - 1), allowed);
        }
        return allowed;
    }
    @Inject(method = "insertItem", at = @At("RETURN"))
    private void jem$afterInsert(int slot, ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        if (jem$suppressedFilterDepth > 0) { jem$suppressedFilterDepth--; return; }
        if (!jem$filterInsertions.isEmpty()) TomsFilterProof.inserted(jem$filterInsertions.remove(jem$filterInsertions.size() - 1), callback.getReturnValue());
    }
}
