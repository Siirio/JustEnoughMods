package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryBasinReceipt;
import com.justenoughmods.achievementguide.integration.create.FactoryIngredients;
import com.justenoughmods.achievementguide.integration.create.FactoryRecipeProof;
import com.justenoughmods.achievementguide.integration.create.FactoryStockCommit;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class FactoryBasinQuestMixin implements FactoryBasinReceipt {
    @Shadow protected List<ItemStack> spoutputBuffer;
    @Unique private static final String jem$pendingKey = "jemFactoryFinishedProductV4";
    @Unique private CompoundTag jem$recipeOrigin = new CompoundTag();
    @Unique private String jem$recipeId;
    @Unique private int jem$produced;
    @Unique private int jem$candidateOutput;
    @Unique private List<FactoryStockCommit.Stock> jem$recipeStocks = List.of();
    @Unique private CompoundTag jem$exportProof;
    @Unique private int jem$exportSent;
    @Unique private int jem$exportStocked;
    @Unique private BasinBlockEntity jem$basin() { return (BasinBlockEntity)(Object)this; }
    @Inject(method = "lambda$new$0", at = @At("RETURN"))
    private void jem$nativeInputMutation(Integer slot, CallbackInfo callback) {
        FactoryIngredients.consumed(jem$basin());
    }
    @Unique private int jem$bufferCount(ItemStack product) {
        if (spoutputBuffer.size() > 256) return -1;
        int count = 0;
        for (ItemStack stack : spoutputBuffer) if (ItemStack.isSameItemSameTags(stack, product)) count += stack.getCount();
        return count;
    }
    @Override public void jem$factoryBegin(Recipe<?> recipe) {
        var basin = jem$basin();
        jem$recipeOrigin = new CompoundTag(); jem$produced = 0; jem$candidateOutput = 0; jem$recipeStocks = List.of();
        if (basin.getPersistentData().contains(jem$pendingKey)) {
            basin.getPersistentData().remove(jem$pendingKey); basin.setChanged();
        }
        if (!(basin.getLevel() instanceof ServerLevel level) || basin.isVirtual()
                || recipe instanceof BasinRecipe nativeRecipe && !nativeRecipe.getFluidIngredients().isEmpty()) return;
        CompoundTag origin = FactoryIngredients.basin(basin);
        if (FactoryRecipeProof.panel(level, origin) == null) return;
        ItemStack product = ItemStack.of(origin.getCompound("product"));
        if (FactoryStockCommit.count(basin.getOutputInventory(), product) != 0 || jem$bufferCount(product) != 0) return;
        jem$recipeOrigin = origin; jem$recipeId = recipe.getId().toString();
        if (basin.getBlockState().getValue(BasinBlock.FACING) == Direction.DOWN) {
            jem$recipeStocks = FactoryStockCommit.before(basin, basin.getOutputInventory(), origin);
        }
    }
    @Inject(method = "acceptOutputs", at = @At("HEAD"))
    private void jem$actualFactoryOutputs(List<ItemStack> items, List<FluidStack> fluids, boolean simulate,
                                          CallbackInfoReturnable<Boolean> callback) {
        jem$candidateOutput = 0;
        if (!simulate && !jem$recipeOrigin.isEmpty() && items.size() <= 256) {
            ItemStack product = ItemStack.of(jem$recipeOrigin.getCompound("product"));
            for (ItemStack stack : items) if (ItemStack.isSameItemSameTags(stack, product)) jem$candidateOutput += stack.getCount();
        }
    }
    @Inject(method = "acceptOutputs", at = @At("RETURN"))
    private void jem$acceptedFactoryOutputs(List<ItemStack> items, List<FluidStack> fluids, boolean simulate,
                                            CallbackInfoReturnable<Boolean> callback) {
        if (!simulate && callback.getReturnValue() && jem$candidateOutput > 0) jem$produced += jem$candidateOutput;
        jem$candidateOutput = 0;
    }
    @Override public void jem$factoryFinish(boolean success) {
        var basin = jem$basin();
        CompoundTag origin = jem$recipeOrigin;
        jem$recipeOrigin = new CompoundTag();
        if (success) FactoryIngredients.consumed(basin);
        if (!success || origin.isEmpty() || jem$produced < origin.getInt("batch") || !FactoryRecipeProof.counts(basin.inputInventory).isEmpty()) return;
        ItemStack product = ItemStack.of(origin.getCompound("product"));
        if (basin.getBlockState().getValue(BasinBlock.FACING) == Direction.DOWN) {
            if (FactoryStockCommit.count(basin.getOutputInventory(), product) == jem$produced
                    && FactoryStockCommit.credited(basin, origin, jem$recipeStocks, jem$produced)) FactoryStockCommit.complete(basin, origin);
        } else if (jem$bufferCount(product) == jem$produced) {
            CompoundTag output = new CompoundTag(); output.put("origin", origin.copy()); output.putString("recipe", jem$recipeId);
            output.putInt("produced", jem$produced); output.putInt("remaining", jem$produced); output.putInt("delivered", 0);
            basin.getPersistentData().put(jem$pendingKey, output); basin.setChanged();
        }
    }

    @Inject(method = "tryClearingSpoutputOverflow", at = @At("HEAD"))
    private void jem$beforeFactoryOverflow(CallbackInfo callback) {
        jem$exportSent = 0; jem$exportStocked = 0;
        jem$exportProof = jem$basin().getPersistentData().getCompound(jem$pendingKey).copy();
        if (jem$exportProof.isEmpty() || jem$bufferCount(ItemStack.of(jem$exportProof.getCompound("origin").getCompound("product"))) != jem$exportProof.getInt("remaining")) {
            jem$exportProof = new CompoundTag(); jem$basin().getPersistentData().remove(jem$pendingKey);
        }
    }
    @Redirect(method = "tryClearingSpoutputOverflow", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItemStacked(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$actualFactoryOverflowInsert(IItemHandler inventory, ItemStack offered, boolean simulate) {
        var basin = jem$basin();
        CompoundTag origin = jem$exportProof.getCompound("origin");
        ItemStack product = ItemStack.of(origin.getCompound("product"));
        int offeredCount = offered.getCount();
        boolean observed = !simulate && !origin.isEmpty() && ItemStack.isSameItemSameTags(offered, product);
        BlockEntity target = null;
        if (observed && basin.getLevel() instanceof ServerLevel level) {
            Direction facing = basin.getBlockState().getValue(BasinBlock.FACING);
            var pos = basin.getBlockPos().below().relative(facing);
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null) target = level.getBlockEntity(pos);
        }
        List<FactoryStockCommit.Stock> stocks = target == null ? List.of() : FactoryStockCommit.before(target, inventory, origin);
        int before = observed ? FactoryStockCommit.count(inventory, product) : -1;
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, offered, simulate);
        int moved = offeredCount - remainder.getCount();
        if (observed && before >= 0 && moved > 0 && FactoryStockCommit.count(inventory, product) - before == moved) {
            jem$exportSent += moved;
            if (FactoryStockCommit.credited(basin, origin, stocks, moved)) jem$exportStocked += moved;
        }
        return remainder;
    }
    @Inject(method = "tryClearingSpoutputOverflow", at = @At("RETURN"))
    private void jem$finishedFactoryOverflow(CallbackInfo callback) {
        var basin = jem$basin();
        if (jem$exportProof == null || jem$exportProof.isEmpty()) return;
        CompoundTag origin = jem$exportProof.getCompound("origin");
        int remaining = jem$bufferCount(ItemStack.of(origin.getCompound("product")));
        if (remaining < 0 || jem$exportProof.getInt("remaining") - remaining != jem$exportSent) {
            basin.getPersistentData().remove(jem$pendingKey); basin.setChanged(); jem$exportProof = null; return;
        }
        jem$exportProof.putInt("remaining", remaining);
        jem$exportProof.putInt("delivered", jem$exportProof.getInt("delivered") + jem$exportStocked);
        if (remaining == 0) {
            if (jem$exportProof.getInt("delivered") == jem$exportProof.getInt("produced")) FactoryStockCommit.complete(basin, origin);
            basin.getPersistentData().remove(jem$pendingKey);
        } else basin.getPersistentData().put(jem$pendingKey, jem$exportProof);
        basin.setChanged(); jem$exportProof = null;
    }
}
