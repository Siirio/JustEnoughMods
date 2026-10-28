package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.mixin.CachedInventoryQuestAccess;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

/** Personal insert-then-retrieve proof on the same real native two-storage controller. */
public final class StorageControllerProof {
    private static final String KEY = "jemQuestLinkedWallV1";
    private static final String SOURCE = KEY + "SourceIdentity";
    public record Before(StorageBlockEntity storage, ControllerBlockEntity controller, Map<ItemTransferObservation.Kind, Long> stock,
                         Map<ItemTransferObservation.Kind, Long> held) {}
    private StorageControllerProof() {}

    public static Before before(StorageContainerMenu menu, ServerPlayer actor) {
        if (!eligible(actor) || actor.containerMenu != menu || !menu.stillValid(actor)) return null;
        StorageBlockEntity storage = menu.getStorageBlockEntity();
        Level level = storage.getLevel();
        BlockPos position = storage.getControllerPos().orElse(null);
        if (level != actor.level() || storage.isLinkedStorage() || position == null || !level.isLoaded(position)
                || !(level.getBlockEntity(position) instanceof ControllerBlockEntity controller) || !linked(controller, storage)) return null;
        var inventory = storage.getStorageWrapper().getInventoryHandler();
        if (inventory.getSlots() > 4096) return null;
        return new Before(storage, controller, stock(inventory), held(actor, menu));
    }

    public static void inserted(StorageContainerMenu menu, ServerPlayer actor, Before before) {
        if (before == null || actor.containerMenu != menu || menu.getStorageBlockEntity() != before.storage || !menu.stillValid(actor)
                || !linked(before.controller, before.storage)) return;
        var inventory = before.storage.getStorageWrapper().getInventoryHandler();
        if (inventory.getSlots() > 4096) return;
        Map<ItemTransferObservation.Kind, Long> stock = stock(inventory), held = held(actor, menu);
        for (var entry : stock.entrySet()) {
            long incoming = entry.getValue() - before.stock.getOrDefault(entry.getKey(), 0L);
            long debit = before.held.getOrDefault(entry.getKey(), 0L) - held.getOrDefault(entry.getKey(), 0L);
            if (incoming < 1 || incoming != debit) continue;
            if (!before.storage.getPersistentData().hasUUID(SOURCE)) {
                before.storage.getPersistentData().putUUID(SOURCE, java.util.UUID.randomUUID()); before.storage.setChanged();
            }
            ListTag previous = before.controller.getPersistentData().getList(KEY, 10);
            ListTag retained = new ListTag();
            for (var raw : previous) {
                CompoundTag row = (CompoundTag) raw;
                if (retained.size() >= 63) break;
                if (row.hasUUID("actor") && row.getUUID("actor").equals(actor.getUUID())
                        && ItemStack.isSameItemSameTags(ItemStack.of(row.getCompound("item")), stack(entry.getKey()))) continue;
                retained.add(row.copy());
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("actor", actor.getUUID()); row.putUUID("source", before.storage.getPersistentData().getUUID(SOURCE));
            row.putLong("position", before.storage.getBlockPos().asLong()); row.put("item", stack(entry.getKey()).save(new CompoundTag()));
            retained.add(row); before.controller.getPersistentData().put(KEY, retained); before.controller.setChanged();
        }
    }

    public static void extracted(NativeTerminalExtractionEvent event) {
        if (event.quantity() < 1 || !eligible(event.actor())) return;
        Object network = event.network();
        // Native sided Controller capability is one read-only cached-insert wrapper around that same controller.
        if (network instanceof CachedInventoryQuestAccess cached) network = cached.jem$nativeCachedInventory().get();
        if (!(network instanceof ControllerBlockEntity controller) || controller.getLevel() != event.actor().level() || controller.isRemoved()) return;
        ListTag rows = controller.getPersistentData().getList(KEY, 10);
        if (rows.size() > 64) return;
        for (int index = 0; index < rows.size(); index++) {
            CompoundTag row = rows.getCompound(index);
            if (!row.hasUUID("actor") || !row.getUUID("actor").equals(event.actor().getUUID()) || !row.hasUUID("source")
                    || !ItemStack.isSameItemSameTags(event.item(), ItemStack.of(row.getCompound("item")))) continue;
            BlockPos position = BlockPos.of(row.getLong("position"));
            if (!controller.getLevel().isLoaded(position) || !(controller.getLevel().getBlockEntity(position) instanceof StorageBlockEntity storage)
                    || !storage.getPersistentData().hasUUID(SOURCE) || !row.getUUID("source").equals(storage.getPersistentData().getUUID(SOURCE))
                    || !linked(controller, storage)) continue;
            rows.remove(index); controller.setChanged();
            QuestActionTrigger.INSTANCE.emit(event.actor(), "v3_store_linked_wall");
            return;
        }
    }

    private static boolean linked(ControllerBlockEntity controller, StorageBlockEntity source) {
        if (controller.isRemoved() || source.isRemoved() || controller.getLevel() == null || controller.getLevel() != source.getLevel()
                || source.isLinkedStorage() || !source.getControllerPos().filter(controller.getBlockPos()::equals).isPresent()
                || !source.isControllerStorageAccessible()) return false;
        List<BlockPos> positions = controller.getStoragePositions();
        if (positions.size() < 2 || positions.size() > 256 || !positions.contains(source.getBlockPos())) return false;
        for (BlockPos position : positions) {
            if (position.equals(source.getBlockPos()) || !controller.getLevel().isLoaded(position)) continue;
            if (controller.getLevel().getBlockEntity(position) instanceof StorageBlockEntity other && !other.isLinkedStorage()
                    && other.isControllerStorageAccessible() && other.getStorageWrapper() != source.getStorageWrapper()
                    && other.getControllerPos().filter(controller.getBlockPos()::equals).isPresent()) return true;
        }
        return false;
    }
    private static Map<ItemTransferObservation.Kind, Long> stock(IItemHandler inventory) {
        Map<ItemTransferObservation.Kind, Long> result = new HashMap<>();
        for (int index = 0; index < inventory.getSlots(); index++) add(result, inventory.getStackInSlot(index));
        return result;
    }
    private static Map<ItemTransferObservation.Kind, Long> held(ServerPlayer actor, StorageContainerMenu menu) {
        Map<ItemTransferObservation.Kind, Long> result = new HashMap<>();
        for (int index = 0; index < actor.getInventory().getContainerSize(); index++) add(result, actor.getInventory().getItem(index));
        add(result, menu.getCarried()); return result;
    }
    private static void add(Map<ItemTransferObservation.Kind, Long> counts, ItemStack stack) {
        if (!stack.isEmpty()) counts.merge(new ItemTransferObservation.Kind(stack.getItem(), stack.hasTag() ? stack.getTag().copy() : new CompoundTag()), (long) stack.getCount(), Long::sum);
    }
    private static ItemStack stack(ItemTransferObservation.Kind kind) {
        ItemStack stack = new ItemStack(kind.item()); if (!kind.tag().isEmpty()) stack.setTag(kind.tag().copy()); return stack;
    }
    private static boolean eligible(ServerPlayer actor) { return !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild; }
}
