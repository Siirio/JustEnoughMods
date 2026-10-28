package com.justenoughmods.achievementguide.integration.create;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

/** Native product-network inventory identity and actual conserved post-recipe stock credit. */
public final class FactoryStockCommit {
    public record Stock(PackagerBlockEntity packager, IItemHandler inventory, int before) {}
    private FactoryStockCommit() {}
    public static int count(IItemHandler inventory, ItemStack product) {
        if (inventory == null || inventory.getSlots() > 256) return -1;
        long amount = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, product)) amount += stack.getCount();
        }
        return (int)Math.min(Integer.MAX_VALUE, amount);
    }
    public static List<Stock> before(BlockEntity target, IItemHandler actualDestination, CompoundTag origin) {
        if (!(target.getLevel() instanceof ServerLevel level) || actualDestination == null || actualDestination.getSlots() > 256
                || FactoryRecipeProof.panel(level, origin) == null) return List.of();
        ItemStack product = ItemStack.of(origin.getCompound("product"));
        var links = LogisticallyLinkedBehaviour.getAllPresent(origin.getUUID("network"), false);
        if (links.size() > 128) return List.of();
        List<Stock> result = new ArrayList<>();
        for (var link : links) {
            if (link.getWorld() != level || !(link.blockEntity instanceof PackagerLinkBlockEntity nativeLink)) continue;
            boolean loaded = true;
            for (Direction side : Direction.values()) {
                var pos = nativeLink.getBlockPos().relative(side);
                if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) loaded = false;
            }
            if (!loaded) continue;
            PackagerBlockEntity packager = nativeLink.getPackager();
            if (packager == null) continue;
            var face = packager.targetInventory.getTarget();
            if (face == null) continue;
            var pos = face.getConnectedPos();
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            IItemHandler inventory = packager.targetInventory.getInventory();
            if (inventory == null || inventory.getSlots() > 256) continue;
            if (!pos.equals(target.getBlockPos()) && !packager.isTargetingSameInventory(new IdentifiedInventory(null, actualDestination))) continue;
            if (result.stream().noneMatch(stock -> stock.inventory == inventory)) result.add(new Stock(packager, inventory, count(inventory, product)));
        }
        return result;
    }
    public static boolean credited(BlockEntity owner, CompoundTag origin, List<Stock> stocks, int actualProduct) {
        if (actualProduct <= 0 || !(owner.getLevel() instanceof ServerLevel level) || FactoryRecipeProof.panel(level, origin) == null) return false;
        ItemStack product = ItemStack.of(origin.getCompound("product"));
        for (Stock stock : stocks) {
            if (stock.packager.getLevel() == level && stock.packager.targetInventory.getInventory() == stock.inventory
                    && count(stock.inventory, product) - stock.before == actualProduct) return true;
        }
        return false;
    }
    public static void complete(BlockEntity owner, CompoundTag origin) {
        if (!(owner.getLevel() instanceof ServerLevel level) || FactoryRecipeProof.panel(level, origin) == null) return;
        var actor = level.getServer().getPlayerList().getPlayer(origin.getUUID("actor"));
        if (TravelProof.eligible(actor)) QuestActionTrigger.INSTANCE.emit(actor, "v3_eng_factory_recipe");
    }
}
