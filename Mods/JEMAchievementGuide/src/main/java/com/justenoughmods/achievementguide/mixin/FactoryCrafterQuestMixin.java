package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryGridReceipt;
import com.justenoughmods.achievementguide.integration.create.FactoryIngredients;
import com.justenoughmods.achievementguide.integration.create.FactoryStockCommit;
import com.justenoughmods.achievementguide.integration.create.CreateCargoTransfers;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class FactoryCrafterQuestMixin {
    @Shadow protected RecipeGridHandler.GroupedItems groupedItems;
    @Shadow protected int countDown;
    @Unique private CompoundTag jem$factoryBeginning;
    @Unique private FactoryGridReceipt jem$factoryExportGroup;
    @Unique private CompoundTag jem$factoryProduct;
    @Unique private int jem$factorySent;
    @Unique private int jem$factoryStocked;
    @Inject(method = "begin", at = @At("HEAD"))
    private void jem$factoryBegin(CallbackInfo callback) { jem$factoryBeginning = FactoryIngredients.beginning((MechanicalCrafterBlockEntity)(Object)this); }
    @Inject(method = "begin", at = @At("RETURN"))
    private void jem$factoryBegan(CallbackInfo callback) {
        FactoryIngredients.began((FactoryGridReceipt)(Object)groupedItems, jem$factoryBeginning);
        jem$factoryBeginning = null;
    }
    @Inject(method = "tryInsert", at = @At("HEAD"))
    private void jem$beforeFactoryExport(CallbackInfo callback) {
        jem$factorySent = 0; jem$factoryStocked = 0;
        jem$factoryExportGroup = (FactoryGridReceipt)(Object)groupedItems;
        jem$factoryProduct = jem$factoryExportGroup.jem$factoryReceipt();
        CompoundTag nativeSnapshot = new CompoundTag();
        ((MechanicalCrafterBlockEntity)(Object)this).write(nativeSnapshot, false);
        String phase = nativeSnapshot.getString("Phase");
        if ((!phase.equals("CRAFTING") && !phase.equals("INSERTING"))
                || countDown > 0 || !jem$factoryProduct.getString("phase").equals("product")) jem$factoryProduct = new CompoundTag();
    }
    @Redirect(method = "tryInsert", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/inventory/InvManipulationBehaviour;insert(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$actualFactoryStockInsert(InvManipulationBehaviour inserter, ItemStack item) {
        var crafter = (MechanicalCrafterBlockEntity)(Object)this;
        var inventory = inserter.getInventory();
        CompoundTag origin = jem$factoryProduct.getCompound("origin");
        ItemStack productItem = ItemStack.of(origin.getCompound("product"));
        int offeredCount = item.getCount();
        boolean product = !origin.isEmpty() && ItemStack.isSameItemSameTags(item, productItem);
        BlockEntity target = null;
        if (product && crafter.getLevel() instanceof ServerLevel level) {
            var pos = crafter.getBlockPos().relative(crafter.getTargetDirection());
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null) target = level.getBlockEntity(pos);
        }
        List<FactoryStockCommit.Stock> stocks = target == null ? List.of() : FactoryStockCommit.before(target, inventory, origin);
        int before = product ? FactoryStockCommit.count(inventory, productItem) : -1;
        ItemStack remainder = inserter.insert(item);
        if (product && before >= 0) {
            int sent = offeredCount - remainder.getCount();
            if (sent > 0 && FactoryStockCommit.count(inventory, productItem) - before == sent) {
                jem$factorySent += sent;
                if (FactoryStockCommit.credited(crafter, origin, stocks, sent)) jem$factoryStocked += sent;
            }
        }
        return remainder;
    }
    @Inject(method = "tryInsert", at = @At("RETURN"))
    private void jem$committedFactoryExport(CallbackInfo callback) {
        if (jem$factoryProduct == null || jem$factoryProduct.isEmpty() || jem$factoryExportGroup == null) return;
        CompoundTag origin = jem$factoryProduct.getCompound("origin");
        String kind = CreateCargoTransfers.fingerprint(ItemStack.of(origin.getCompound("product")));
        long remaining = jem$factoryExportGroup.jem$factoryContents().getOrDefault(kind, 0L);
        if (jem$factoryProduct.getInt("remaining") - remaining != jem$factorySent) {
            jem$factoryExportGroup.jem$factoryReceipt(new CompoundTag());
            jem$factoryExportGroup = null; jem$factoryProduct = null; return;
        }
        jem$factoryProduct.putInt("remaining", (int)remaining);
        jem$factoryProduct.putInt("delivered", jem$factoryProduct.getInt("delivered") + jem$factoryStocked);
        if (remaining == 0 && jem$factoryProduct.getInt("delivered") == jem$factoryProduct.getInt("produced")) {
            FactoryStockCommit.complete((BlockEntity)(Object)this, origin);
        } else jem$factoryExportGroup.jem$factoryReceipt(jem$factoryProduct);
        jem$factoryExportGroup = null; jem$factoryProduct = null;
    }
}
