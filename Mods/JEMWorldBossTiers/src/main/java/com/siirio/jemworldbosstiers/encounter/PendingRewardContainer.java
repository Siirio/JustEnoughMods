package com.siirio.jemworldbosstiers.encounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PendingRewardContainer {
    private static final String KEY = "RewardContainers:";

    private PendingRewardContainer() {}

    public static void enqueue(MinecraftServer server, UUID player, String sourceId, String titleKey, List<ItemStack> items) {
        enqueue(server, player, sourceId, titleKey, items, null);
    }

    public static void enqueueGrouped(MinecraftServer server, UUID player, String sourceId, String titleKey, List<RewardGroup> groups) {
        if (groups == null || groups.isEmpty()) return;
        ListTag savedGroups = new ListTag();
        for (RewardGroup group : groups) {
            ListTag items = saveItems(group.items());
            if (items.isEmpty()) continue;
            CompoundTag saved = new CompoundTag();
            saved.putString("TitleKey", group.titleKey());
            saved.put("Items", items);
            savedGroups.add(saved);
        }
        enqueue(server, player, sourceId, titleKey, List.of(), savedGroups);
    }

    private static void enqueue(MinecraftServer server, UUID player, String sourceId, String titleKey,
                                List<ItemStack> items, ListTag groups) {
        HostedRewards storage = HostedRewards.get(server);
        ListTag containers = containers(storage, player);
        ListTag slots = saveItems(items);
        if (slots.isEmpty() && (groups == null || groups.isEmpty())) return;
        CompoundTag container = new CompoundTag();
        container.putUUID("Id", UUID.randomUUID());
        container.putString("SourceId", sourceId);
        container.putString("TitleKey", titleKey);
        container.putLong("CreatedAt", System.currentTimeMillis());
        container.putBoolean("CreatedAtEpoch", true);
        container.put("Items", slots);
        if (groups != null) container.put("Groups", groups);
        containers.add(container);
        storage.pending().put(KEY + player, containers);
        storage.setDirty();
    }

    private static ListTag saveItems(List<ItemStack> items) {
        ListTag slots = new ListTag();
        for (ItemStack item : items) {
            int remaining = item.getCount();
            while (!item.isEmpty() && remaining > 0) {
                int count = Math.min(remaining, item.getMaxStackSize());
                slots.add(item.copyWithCount(count).save(new CompoundTag()));
                remaining -= count;
            }
        }
        return slots;
    }

    static ListTag containers(HostedRewards storage, UUID player) {
        ListTag containers = storage.pending().getList(KEY + player, Tag.TAG_COMPOUND);
        ListTag legacy = storage.pending().getList(player.toString(), Tag.TAG_COMPOUND);
        if (!legacy.isEmpty()) {
            CompoundTag migrated = new CompoundTag();
            migrated.putUUID("Id", UUID.randomUUID());
            migrated.putString("SourceId", "legacy_boss_loot");
            migrated.putString("TitleKey", "reward.jem_world_boss_tiers.boss_loot");
            migrated.putLong("CreatedAt", 0);
            migrated.put("Items", legacy.copy());
            containers.add(migrated);
            storage.pending().put(KEY + player, containers);
            storage.pending().remove(player.toString());
            storage.setDirty();
        }
        return containers;
    }

    public static void importLegacy(ServerPlayer player, UUID id, ListTag items) {
        HostedRewards storage = HostedRewards.get(player.server);
        ListTag containers = containers(storage, player.getUUID());
        for (Tag value : containers) {
            if (id.equals(((CompoundTag) value).getUUID("Id"))) {
                storage.flush(player.server);
                return;
            }
        }
        CompoundTag container = new CompoundTag();
        container.putUUID("Id", id);
        container.putString("SourceId", "legacy_smp_delivery");
        container.putString("TitleKey", "reward.jem_world_boss_tiers.legacy_delivery");
        container.put("Items", items.copy());
        container.putBoolean("ImportPending", true);
        containers.add(container);
        storage.pending().put(KEY + player.getUUID(), containers);
        storage.setDirty();
        storage.flush(player.server);
    }

    public static void finishLegacyImport(ServerPlayer player, UUID id) {
        HostedRewards storage = HostedRewards.get(player.server);
        for (Tag value : containers(storage, player.getUUID())) {
            CompoundTag container = (CompoundTag) value;
            if (!id.equals(container.getUUID("Id"))) continue;
            container.remove("ImportPending");
            storage.setDirty();
            storage.flush(player.server);
            return;
        }
    }

    public static ListTag bundles(ServerPlayer player, int offset, int limit) {
        if (offset < 0 || limit < 1) throw new IllegalArgumentException("Invalid reward page");
        return bundles(player, "", offset, limit);
    }

    public static ListTag bundles(ServerPlayer player, String source) {
        return bundles(player, source, 0, Integer.MAX_VALUE);
    }

    private static ListTag bundles(ServerPlayer player, String source, int offset, int limit) {
        ListTag bundles = new ListTag();
        int visible = 0;
        for (Tag value : containers(HostedRewards.get(player.server), player.getUUID())) {
            CompoundTag container = (CompoundTag) value;
            if (container.getBoolean("ImportPending")) continue;
            String sourceId = container.getString("SourceId");
            if (!source.isEmpty() && !sourceId.equals(source) && !sourceId.startsWith(source + ":")) continue;
            boolean pending = false;
            for (Tag item : allItems(container)) {
                if (((CompoundTag) item).getByte("Count") > 0) { pending = true; break; }
            }
            if (!pending || visible++ < offset) continue;
            if (bundles.size() >= limit) break;
            ListTag items = new ListTag();
            for (Tag item : allItems(container)) {
                if (((CompoundTag) item).getByte("Count") > 0) items.add(item.copy());
            }
            if (items.isEmpty()) continue;
            CompoundTag bundle = new CompoundTag();
            bundle.putUUID("id", container.getUUID("Id"));
            bundle.putString("sourceId", container.getString("SourceId"));
            bundle.putString("titleKey", container.getString("TitleKey"));
            bundle.putLong("createdAt", container.getBoolean("CreatedAtEpoch") ? container.getLong("CreatedAt") : 0);
            bundle.put("items", items);
            if (container.contains("Groups", Tag.TAG_LIST)) bundle.put("groups", visibleGroups(container));
            bundles.add(bundle);
        }
        return bundles;
    }

    public static int count(ServerPlayer player) {
        int count = 0;
        for (Tag value : containers(HostedRewards.get(player.server), player.getUUID())) {
            CompoundTag container = (CompoundTag) value;
            if (container.getBoolean("ImportPending")) continue;
            for (Tag item : allItems(container)) {
                if (((CompoundTag) item).getByte("Count") > 0) { count++; break; }
            }
        }
        return count;
    }

    public static int totalItems(ServerPlayer player) {
        int count = 0;
        for (Tag value : containers(HostedRewards.get(player.server), player.getUUID())) {
            CompoundTag container = (CompoundTag) value;
            if (container.getBoolean("ImportPending")) continue;
            for (Tag item : allItems(container)) {
                count += Math.max(0, ((CompoundTag) item).getByte("Count"));
            }
        }
        return count;
    }

    public static void discardBySource(MinecraftServer server, Collection<UUID> players, String source) {
        HostedRewards storage=HostedRewards.get(server);
        boolean changed=false;
        for(UUID player:players) {
            ListTag containers=containers(storage,player);
            for(int index=containers.size()-1;index>=0;index--) {
                String candidate=containers.getCompound(index).getString("SourceId");
                if(candidate.equals(source) || candidate.startsWith(source+":")) { containers.remove(index);changed=true; }
            }
            if(containers.isEmpty()) storage.pending().remove(KEY+player);
            else storage.pending().put(KEY+player,containers);
        }
        if(changed) storage.setDirty();
    }

    public static boolean claim(ServerPlayer player, UUID bundleId) {
        return claimSelection(player, bundleId == null ? null : Set.of(bundleId));
    }

    public static boolean claimBundles(ServerPlayer player, Collection<UUID> bundleIds) {
        if (bundleIds == null || bundleIds.isEmpty() || bundleIds.stream().anyMatch(java.util.Objects::isNull)) return false;
        return claimSelection(player, new HashSet<>(bundleIds));
    }

    private static boolean claimSelection(ServerPlayer player, Set<UUID> bundleIds) {
        if (!player.isAlive() || player.containerMenu != player.inventoryMenu
                || !player.inventoryMenu.getCarried().isEmpty()) return false;
        HostedRewards storage = HostedRewards.get(player.server);
        ListTag containers = (ListTag) containers(storage, player.getUUID()).copy();
        List<ItemStack> inventory = new ArrayList<>();
        for (ItemStack item : player.getInventory().items) inventory.add(item.copy());
        boolean transferred = false;
        Set<UUID> remaining = bundleIds == null ? null : new HashSet<>(bundleIds);
        for (int index = 0; index < containers.size(); index++) {
            CompoundTag container = containers.getCompound(index);
            if (bundleIds != null && !bundleIds.contains(container.getUUID("Id"))) continue;
            if (container.getBoolean("ImportPending")) return false;
            if (remaining != null) remaining.remove(container.getUUID("Id"));
            for (ListTag items : itemLists(container)) for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                    CompoundTag item = items.getCompound(itemIndex);
                    if (item.getByte("Count") <= 0) continue;
                    ItemStack stack = ItemStack.of(item);
                    if (stack.isEmpty()) continue;
                    int before = stack.getCount();
                    insert(inventory, stack, player.getInventory().getMaxStackSize());
                    if (stack.getCount() == before) continue;
                    transferred = true;
                    if (stack.isEmpty()) item.putByte("Count", (byte) 0);
                    else items.set(itemIndex, stack.save(new CompoundTag()));
                }
        }
        if (!transferred || remaining != null && !remaining.isEmpty()) return false;
        for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().items.set(slot, inventory.get(slot));
        clearCompleted(storage, player.getUUID(), containers);
        storage.setDirty();
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    private static void insert(List<ItemStack> inventory, ItemStack incoming, int inventoryLimit) {
        for (ItemStack existing : inventory) {
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, incoming)) continue;
            int capacity = Math.max(0, Math.min(existing.getMaxStackSize(), inventoryLimit) - existing.getCount());
            int transferred = Math.min(capacity, incoming.getCount());
            existing.grow(transferred);
            incoming.shrink(transferred);
            if (incoming.isEmpty()) return;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            int transferred = Math.min(incoming.getCount(), Math.min(incoming.getMaxStackSize(), inventoryLimit));
            inventory.set(slot, incoming.copyWithCount(transferred));
            incoming.shrink(transferred);
            if (incoming.isEmpty()) return;
        }
    }

    static void clearCompleted(HostedRewards storage, UUID player, ListTag containers) {
        for (int index = containers.size() - 1; index >= 0; index--) {
            boolean pending = false;
            for (Tag item : allItems(containers.getCompound(index))) {
                if (((CompoundTag) item).getByte("Count") > 0) {
                    pending = true;
                    break;
                }
            }
            if (!pending) containers.remove(index);
        }
        if (containers.isEmpty()) storage.pending().remove(KEY + player);
        else storage.pending().put(KEY + player, containers);
    }

    private static List<ListTag> itemLists(CompoundTag container) {
        List<ListTag> lists = new ArrayList<>();
        ListTag direct = container.getList("Items", Tag.TAG_COMPOUND);
        if (!direct.isEmpty()) lists.add(direct);
        for (Tag value : container.getList("Groups", Tag.TAG_COMPOUND))
            lists.add(((CompoundTag) value).getList("Items", Tag.TAG_COMPOUND));
        return lists;
    }

    private static List<Tag> allItems(CompoundTag container) {
        List<Tag> items = new ArrayList<>();
        itemLists(container).forEach(items::addAll);
        return items;
    }

    private static ListTag visibleGroups(CompoundTag container) {
        ListTag result = new ListTag();
        for (Tag value : container.getList("Groups", Tag.TAG_COMPOUND)) {
            CompoundTag group = (CompoundTag) value;
            ListTag items = new ListTag();
            for (Tag item : group.getList("Items", Tag.TAG_COMPOUND))
                if (((CompoundTag) item).getByte("Count") > 0) items.add(item.copy());
            if (items.isEmpty()) continue;
            CompoundTag visible = new CompoundTag();
            visible.putString("titleKey", group.getString("TitleKey"));
            visible.put("items", items);
            result.add(visible);
        }
        return result;
    }
}
