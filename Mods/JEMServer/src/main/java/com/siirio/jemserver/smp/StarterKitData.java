package com.siirio.jemserver.smp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;

/** One account entitlement; player inventory and its operation receipt share one native player save. */
final class StarterKitData extends SavedData {
    private static final String NAME = "jem_starter_kits";
    static final String RECEIPT = "jem_server:starter_kit";
    private final CompoundTag accounts;

    private StarterKitData() { this(new CompoundTag()); }
    private StarterKitData(CompoundTag accounts) { this.accounts = accounts; }

    static StarterKitData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                tag -> new StarterKitData(tag.getCompound("Accounts")), StarterKitData::new, NAME);
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.put("Accounts", accounts.copy());
        return tag;
    }

    void deliver(ServerPlayer player, UUID account) throws IOException {
        String key = account.toString();
        if (!accounts.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag entitlement = new CompoundTag();
            entitlement.put("Remaining", kit());
            accounts.put(key, entitlement);
            flush(player.server);
        }
        CompoundTag row = accounts.getCompound(key);
        if (row.contains("Pending", Tag.TAG_COMPOUND)) reconcile(player, account, row);
        if (row.getBoolean("Complete") || player.containerMenu != player.inventoryMenu
                || !player.inventoryMenu.getCarried().isEmpty()) return;
        List<ItemStack> inventory = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) inventory.add(stack.copy());
        ListTag remaining = row.getList("Remaining", Tag.TAG_COMPOUND).copy();
        boolean changed = false;
        for (int index = 0; index < remaining.size(); index++) {
            ItemStack stack = ItemStack.of(remaining.getCompound(index));
            if (stack.isEmpty()) throw new IOException("Starter kit contains an unavailable saved item");
            int before = stack.getCount();
            insert(inventory, stack, player.getInventory().getMaxStackSize());
            changed |= stack.getCount() != before;
            if (stack.isEmpty()) { remaining.remove(index--); }
            else remaining.set(index, stack.save(new CompoundTag()));
        }
        if (!changed) return;
        UUID operation = UUID.randomUUID();
        CompoundTag pending = new CompoundTag();
        pending.putUUID("Operation", operation);
        pending.putUUID("Player", player.getUUID());
        pending.put("Remaining", remaining);
        row.put("Pending", pending);
        flush(player.server);
        for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().items.set(slot, inventory.get(slot));
        CompoundTag receipt = new CompoundTag();
        receipt.putUUID("Account", account);
        receipt.putUUID("Operation", operation);
        persisted(player).put(RECEIPT, receipt);
        player.getInventory().setChanged();
        savePlayer(player, account, operation);
        commit(player.server, row);
        player.inventoryMenu.broadcastChanges();
    }

    private void reconcile(ServerPlayer player, UUID account, CompoundTag row) throws IOException {
        CompoundTag pending = row.getCompound("Pending");
        UUID operation = pending.getUUID("Operation");
        UUID recipient = pending.getUUID("Player");
        if (matches(persisted(player).getCompound(RECEIPT), account, operation)) {
            savePlayer(player, account, operation);
            commit(player.server, row);
            return;
        }
        if (matches(savedReceipt(player.server, recipient), account, operation)) {
            commit(player.server, row);
            return;
        }
        // A prepared operation without a native receipt never committed its inventory.
        if (!recipient.equals(player.getUUID()) && player.server.getPlayerList().getPlayer(recipient) != null)
            throw new IOException("Starter kit operation still belongs to another connected player");
        row.remove("Pending");
        flush(player.server);
    }

    private void commit(MinecraftServer server, CompoundTag row) throws IOException {
        row.put("Remaining", row.getCompound("Pending").getList("Remaining", Tag.TAG_COMPOUND).copy());
        row.putBoolean("Complete", row.getList("Remaining", Tag.TAG_COMPOUND).isEmpty());
        row.remove("Pending");
        flush(server);
    }

    private void flush(MinecraftServer server) throws IOException {
        setDirty();
        var file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(NAME + ".dat").toFile();
        save(file);
        if (!accounts.equals(NbtIo.readCompressed(file).getCompound("data").getCompound("Accounts"))) {
            setDirty();
            throw new IOException("Starter kit journal was not persisted");
        }
    }

    private static void savePlayer(ServerPlayer player, UUID account, UUID operation) throws IOException {
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        ((com.siirio.jemserver.mixin.PlayerSaveAccess) player.server.getPlayerList()).jem$save(player);
        CompoundTag saved = NbtIo.readCompressed(player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .resolve(player.getUUID() + ".dat").toFile());
        if (!matches(saved.getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG).getCompound(RECEIPT), account, operation)
                || !expected.getList("Inventory", Tag.TAG_COMPOUND).equals(saved.getList("Inventory", Tag.TAG_COMPOUND)))
            throw new IOException("Starter kit inventory receipt was not persisted");
    }

    private static CompoundTag savedReceipt(MinecraftServer server, UUID player) throws IOException {
        Path path = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player + ".dat");
        if (!java.nio.file.Files.exists(path)) return new CompoundTag();
        return NbtIo.readCompressed(path.toFile()).getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG).getCompound(RECEIPT);
    }

    private static boolean matches(CompoundTag receipt, UUID account, UUID operation) {
        return receipt.hasUUID("Account") && receipt.hasUUID("Operation")
                && receipt.getUUID("Account").equals(account) && receipt.getUUID("Operation").equals(operation);
    }

    static CompoundTag persisted(ServerPlayer player) {
        CompoundTag forge = player.getPersistentData();
        if (!forge.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) forge.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return forge.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static ListTag kit() throws IOException {
        List<ItemStack> items = List.of(new ItemStack(Items.COOKED_BEEF, 64), new ItemStack(Items.IRON_INGOT, 10),
                new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.LEATHER_CHESTPLATE),
                new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS),
                nativeItem("backpacked:backpack"), nativeItem("naturescompass:naturescompass"));
        ListTag result = new ListTag();
        for (ItemStack item : items) result.add(item.save(new CompoundTag()));
        return result;
    }

    private static ItemStack nativeItem(String id) throws IOException {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) throw new IOException("Missing starter item " + id);
        return new ItemStack(item);
    }

    private static void insert(List<ItemStack> inventory, ItemStack incoming, int limit) {
        for (ItemStack existing : inventory) {
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, incoming)) continue;
            int count = Math.min(incoming.getCount(), Math.max(0, Math.min(limit, existing.getMaxStackSize()) - existing.getCount()));
            existing.grow(count);
            incoming.shrink(count);
            if (incoming.isEmpty()) return;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            int count = Math.min(incoming.getCount(), Math.min(limit, incoming.getMaxStackSize()));
            inventory.set(slot, incoming.copyWithCount(count));
            incoming.shrink(count);
            if (incoming.isEmpty()) return;
        }
    }
}
