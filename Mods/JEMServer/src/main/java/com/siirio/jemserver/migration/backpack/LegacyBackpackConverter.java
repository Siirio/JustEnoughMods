package com.siirio.jemserver.migration.backpack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Raw-NBT conversion, before missing legacy registrations can erase item stacks. */
public final class LegacyBackpackConverter {
    private static final String OLD = "backpacked:backpack";
    private static final String NEW = "sophisticatedbackpacks:backpack";
    private static final String SYNC = "framework:synced_entity_data";
    private static final int MAX_DEPTH = 128;

    private LegacyBackpackConverter() {}

    public static final class Result {
        private long backpacks;
        private final List<Refund> refunds = new ArrayList<>();

        public long backpacks() { return backpacks; }
        public List<Refund> refunds() { return List.copyOf(refunds); }
    }

    public record Refund(UUID owner, UUID receipt, ListTag items) {}

    public static boolean convertTree(CompoundTag root, String source, Result result) throws IOException {
        return walk(root, source, result, 0, false);
    }

    /** Only the legacy obligations still consumed by RewardsData, never archived entity snapshots. */
    public static boolean convertLegacyRewards(CompoundTag root, String source, Result result) throws IOException {
        CompoundTag data = optionalCompound(root, "data", source);
        boolean changed = false;
        if (source.endsWith("jem_hosted_boss_rewards.dat")) {
            for (String key : new ArrayList<>(data.getAllKeys())) {
                if (key.startsWith("RewardContainers:")) {
                    try { UUID.fromString(key.substring("RewardContainers:".length())); }
                    catch (IllegalArgumentException malformed) { throw failure(source, "invalid legacy reward owner"); }
                    ListTag containers = optionalList(data, key, source);
                    for (int i = 0; i < containers.size(); i++) {
                        CompoundTag row = compound(containers.get(i), source + "/" + key + "/" + i);
                        String path = source + "/" + key + "/" + i;
                        changed |= walk(optionalList(row, "Items", path), path + "/Items", result, 0, false);
                        ListTag groups = optionalList(row, "Groups", path);
                        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                            CompoundTag group = compound(groups.get(groupIndex), path);
                            changed |= walk(optionalList(group, "Items", path), path + "/Groups/" + groupIndex + "/Items", result, 0, false);
                        }
                    }
                } else {
                    try { UUID.fromString(key); }
                    catch (IllegalArgumentException unrelated) { continue; }
                    changed |= walk(optionalList(data, key, source), source + "/" + key, result, 0, false);
                }
            }
        } else if (source.endsWith("jem_smp_v2.dat")) {
            if (data.getInt("dataVersion") > 1) throw failure(source, "unsupported future SMP legacy data version");
            ListTag profiles = optionalList(optionalCompound(data, "tables", source), "profiles", source);
            for (int i = 0; i < profiles.size(); i++) {
                CompoundTag profile = compound(profiles.get(i), source);
                if (!profile.hasUUID("id")) continue;
                String path = source + "/tables/profiles/" + i;
                changed |= walk(optionalList(profile, "deliveries", path), path + "/deliveries", result, 0, false);
                CompoundTag claim = optionalCompound(profile, "deliveryClaim", path);
                if (claim.hasUUID("id")) changed |= walk(optionalList(claim, "remaining", path),
                        path + "/deliveryClaim/remaining", result, 0, false);
                CompoundTag migration = optionalCompound(profile, "rewardMigration", path);
                if (migration.hasUUID("id")) changed |= walk(optionalList(migration, "items", path),
                        path + "/rewardMigration/items", result, 0, false);
            }
        } else throw failure(source, "unsupported legacy reward source");
        return changed;
    }

    public static CompoundTag convertItem(CompoundTag item, String source, Result result) throws IOException {
        CompoundTag copy = item.copy();
        requireItem(copy, source);
        walk(copy, source, result, 0, true);
        return copy;
    }

    private static boolean walk(Tag value, String path, Result result, int depth, boolean validate) throws IOException {
        if (depth > MAX_DEPTH) throw failure(path, "NBT nesting exceeds " + MAX_DEPTH);
        boolean changed = false;
        if (value instanceof CompoundTag compound) {
            boolean item = isItem(compound);
            // Native reward journals retain consumed stacks as permanent receipts.
            if (item && compound.getInt("Count") == 0) return false;
            if (validate && compound.contains("id", Tag.TAG_STRING) && compound.contains("Count") && !item) {
                throw failure(path, "malformed nested item count");
            }
            if (item && OLD.equals(compound.getString("id"))) {
                requireItem(compound, path);
                convertBackpack(compound, path, result, depth);
                return true;
            }
            if (item && isFurniture(compound.getString("id"))) {
                requireItem(compound, path);
                convertFurniture(compound, path, result, depth);
                return true;
            }
            if (item && validate) validateItem(compound, path);
            for (String key : new ArrayList<>(compound.getAllKeys())) {
                changed |= walk(compound.get(key), path + "/" + key, result, depth + 1, validate);
            }
        } else if (value instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) changed |= walk(list.get(i), path + "/" + i, result, depth + 1, validate);
        }
        return changed;
    }

    private static void convertBackpack(CompoundTag item, String path, Result result, int depth) throws IOException {
        if (item.getInt("Count") != 1) throw failure(path, "legacy backpack count must be one");
        CompoundTag tag = optionalCompound(item, "tag", path).copy();
        ListTag contents = new ListTag();
        Set<Integer> occupied = new HashSet<>();
        int capacity = 45;
        if (tag.contains("Backpacked_BackpackSlots")) {
            CompoundTag slots = optionalCompound(tag, "Backpacked_BackpackSlots", path);
            if (!slots.contains("MaxSlots", Tag.TAG_INT)) throw failure(path, "invalid original capacity");
            capacity = Math.max(capacity, slots.getInt("MaxSlots"));
        }
        if (capacity > 4096) throw failure(path, "unreasonable original capacity");
        for (Tag entry : optionalList(tag, "Items", path)) {
            CompoundTag stack = compound(entry, path + "/Items").copy();
            requireItem(stack, path + "/Items");
            int slot = slot(stack, path);
            if (!occupied.add(slot)) throw failure(path, "duplicate original slot " + slot);
            walk(stack, path + "/Items/" + slot, result, depth + 1, true);
            stack.putInt("Slot", slot);
            contents.add(stack);
            capacity = Math.max(capacity, slot + 1);
        }
        int next = capacity;
        for (String auxiliary : List.of("JemWorkbenchIngredients", "JemFurnace")) {
            CompoundTag storage = optionalCompound(tag, auxiliary, path);
            Set<Integer> auxiliarySlots = new HashSet<>();
            int limit = auxiliary.equals("JemFurnace") ? 3 : 9;
            for (Tag entry : optionalList(storage, "Items", path + "/" + auxiliary)) {
                CompoundTag stack = compound(entry, path + "/" + auxiliary).copy();
                requireItem(stack, path + "/" + auxiliary);
                int oldSlot = slot(stack, path);
                if (oldSlot >= limit || !auxiliarySlots.add(oldSlot)) throw failure(path, "invalid auxiliary slot");
                walk(stack, path + "/" + auxiliary + "/" + oldSlot, result, depth + 1, true);
                stack.putInt("Slot", next++);
                contents.add(stack);
            }
        }
        int size = Math.max(27, ((next + 8) / 9) * 9);
        for (String key : List.of("Items", "JemWorkbenchIngredients", "JemFurnace", "Augments",
                "Backpacked_BackpackSlots", "Backpacked_AugmentBays", "contentsUuid")) tag.remove(key);
        // Remaining metadata can contain containers or capability-owned item stacks too.
        walk(tag, path + "/tag", result, depth + 1, true);
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("Size", size);
        inventory.put("Items", contents);
        tag.putInt("inventorySlots", size);
        tag.putInt("upgradeSlots", 1);
        tag.put("inventory", inventory);
        item.putString("id", NEW);
        item.putByte("Count", (byte) 1);
        item.put("tag", tag);
        // ForgeCaps belongs to the stack itself, outside its ordinary tag.
        for (String key : new ArrayList<>(item.getAllKeys())) {
            if (!key.equals("tag")) walk(item.get(key), path + "/" + key, result, depth + 1, true);
        }
        validateItem(item, path);
        result.backpacks++;
    }

    private static boolean isFurniture(String id) {
        return id.equals("backpacked:backpack_dock") || id.equals("backpacked:oak_backpack_shelf")
                || id.equals("backpacked:spruce_backpack_shelf") || id.equals("backpacked:birch_backpack_shelf")
                || id.equals("backpacked:jungle_backpack_shelf") || id.equals("backpacked:acacia_backpack_shelf")
                || id.equals("backpacked:dark_oak_backpack_shelf") || id.equals("backpacked:cherry_backpack_shelf")
                || id.equals("backpacked:crimson_backpack_shelf") || id.equals("backpacked:warped_backpack_shelf");
    }

    private static void convertFurniture(CompoundTag item, String path, Result result, int depth) throws IOException {
        CompoundTag tag = optionalCompound(item, "tag", path).copy();
        CompoundTag old = optionalCompound(tag, "BlockEntityTag", path);
        ListTag items = new ListTag();
        CompoundTag bag = optionalCompound(old, "Backpack", path).copy();
        if (bag.getString("id").equals("minecraft:air") && bag.getInt("Count") == 0) {
            if (bag.contains("tag") || bag.contains("ForgeCaps"))
                throw failure(path, "empty furniture backpack has retained metadata");
            bag = new CompoundTag();
        }
        ListTag cached = optionalList(optionalCompound(old, "Container", path), "Items", path);
        if (cached.size() > 1 || !cached.isEmpty() && slot(compound(cached.get(0), path), path) != 0)
            throw failure(path, "unexpected cached furniture slots");
        if (bag.isEmpty() && !cached.isEmpty() && !emptyEquipped(cached.getCompound(0)))
            throw failure(path, "cached furniture backpack has no authoritative root snapshot");
        if (!bag.isEmpty()) {
            requireItem(bag, path + "/Backpack");
            if (old.contains("Items")) {
                CompoundTag bagTag = optionalCompound(bag, "tag", path).copy();
                bagTag.put("Items", optionalList(old, "Items", path).copy());
                bag.put("tag", bagTag);
            }
            walk(bag, path + "/Backpack", result, depth + 1, true);
            bag.putByte("Slot", (byte) 0);
            items.add(bag);
        } else if (!optionalList(old, "Items", path).isEmpty()) {
            throw failure(path, "furniture has contents without its backpack");
        }
        CompoundTag nativeBlock = new CompoundTag();
        nativeBlock.putString("id", "minecraft:barrel");
        nativeBlock.put("Items", items);
        // The shelf's Container is a cached view of Backpack, not another inventory.
        for (String key : List.of("CustomName", "Lock")) if (old.contains(key)) nativeBlock.put(key, old.get(key).copy());
        tag.put("BlockEntityTag", nativeBlock);
        walk(tag, path + "/tag", result, depth + 1, true);
        item.putString("id", "minecraft:barrel");
        item.put("tag", tag);
        for (String key : new ArrayList<>(item.getAllKeys())) {
            if (!key.equals("tag")) walk(item.get(key), path + "/" + key, result, depth + 1, true);
        }
        validateItem(item, path);
    }

    public static boolean convertPlayer(CompoundTag root, UUID player, String source, Result result) throws IOException {
        List<CompoundTag> current = new ArrayList<>();
        List<CompoundTag> legacy = new ArrayList<>();
        CompoundTag caps = optionalCompound(root, "ForgeCaps", source);
        ListTag rows = optionalList(caps, SYNC, source);
        ListTag retained = new ListTag();
        boolean captured = false;
        boolean currentRowSeen = false;
        for (Tag entry : rows) {
            CompoundTag row = compound(entry, source + "/" + SYNC);
            String key = row.getString("DataKey");
            if (!key.startsWith("backpacked:")) { retained.add(row.copy()); continue; }
            captured = true;
            if (!"minecraft:player".equals(row.getString("ClassKey"))) throw failure(source, "unexpected Backpacked synced owner");
            if (key.equals("backpacked:backpacks")) {
                if (!(row.get("Value") instanceof ListTag values)) throw failure(source, "invalid equipped backpack list");
                if (currentRowSeen) throw failure(source, "duplicate equipped backpack rows");
                currentRowSeen = true;
                collectEquipped(values, current, source);
            } else if (key.equals("backpacked:backpack")) {
                CompoundTag value = compound(row.get("Value"), source);
                if (!emptyEquipped(value)) { requireItem(value, source); legacy.add(withoutSlot(value)); }
            } else if (!Set.of("backpacked:selected_backpack", "backpacked:selected_backpack_index",
                    "backpacked:unlockable_backpack_slots", "backpacked:immortal_cooldown",
                    "backpacked:cosmetic_backpack", "backpacked:unlock_tracker", "backpacked:backpack_scale",
                    "backpacked:trader_pickpocketing").contains(key)) {
                throw failure(source, "unknown Backpacked synced field " + key);
            }
        }
        ListTag inventory = optionalList(root, "Inventory", source).copy();
        ListTag remaining = new ListTag();
        boolean[] occupied = new boolean[36];
        for (Tag entry : inventory) {
            CompoundTag stack = compound(entry, source + "/Inventory");
            int slot = slot(stack, source);
            if (slot == 200) {
                if (!emptyEquipped(stack)) {
                    requireItem(stack, source);
                    legacy.add(withoutSlot(stack));
                }
                captured = true;
            } else {
                remaining.add(stack.copy());
                if (slot < occupied.length) {
                    if (occupied[slot]) throw failure(source, "duplicate player inventory slot " + slot);
                    occupied[slot] = true;
                }
            }
        }
        // Upstream moves and clears these owners, it never writes mirrored snapshots.
        List<CompoundTag> selected = new ArrayList<>(current);
        selected.addAll(legacy);
        if (captured) {
            caps.put(SYNC, retained);
            root.put("ForgeCaps", caps);
            root.put("Inventory", remaining);
        }
        boolean changed = convertTree(root, source, result) | captured;
        int index = 0;
        for (CompoundTag old : selected) {
            if (!OLD.equals(old.getString("id"))) throw failure(source, "non-backpack in equipped storage");
            CompoundTag bag = convertItem(old, source + "/equipped/" + index, result);
            int free = 0;
            while (free < occupied.length && occupied[free]) free++;
            if (free < occupied.length) {
                occupied[free] = true;
                bag.putByte("Slot", (byte) free);
                remaining.add(bag);
            } else refund(player, source + "/equipped/" + index, bag, result);
            index++;
        }
        if (captured) root.put("Inventory", remaining);
        return changed;
    }

    private static void collectEquipped(ListTag values, List<CompoundTag> bags, String source) throws IOException {
        for (Tag value : values) {
            CompoundTag stack = compound(value, source);
            if (emptyEquipped(stack)) continue;
            requireItem(stack, source);
            bags.add(withoutSlot(stack));
        }
    }

    private static CompoundTag withoutSlot(CompoundTag stack) {
        CompoundTag copy = stack.copy();
        copy.remove("Slot");
        if (copy.get("Count") instanceof NumericTag) copy.putInt("Count", copy.getInt("Count"));
        return copy;
    }

    private static boolean emptyEquipped(CompoundTag stack) {
        return stack.isEmpty() || stack.getString("id").equals("minecraft:air")
                || stack.get("Count") instanceof NumericTag && stack.getInt("Count") <= 0;
    }

    public static boolean convertQueue(CompoundTag root, String source, Result result) throws IOException {
        CompoundTag data = optionalCompound(root, "data", source);
        boolean changed = false;
        if (source.contains("backpacked_recall")) {
            ListTag queues = optionalList(data, "RecallQueues", source);
            int dimensionIndex = 0;
            for (Tag dimension : queues) {
                int shelfIndex = 0;
                for (Tag shelf : optionalList(compound(dimension, source), "ShelfQueues", source)) {
                    int ownerIndex = 0;
                    for (Tag owner : optionalList(compound(shelf, source), "PlayerQueues", source)) {
                        CompoundTag queue = compound(owner, source);
                        UUID uuid = uuid(queue, "Owner", source);
                        int bagIndex = 0;
                        for (Tag entry : optionalList(queue, "Backpacks", source)) {
                            CompoundTag wrapper = compound(entry, source);
                            CompoundTag old = optionalCompound(wrapper, "Item", source);
                            if (!OLD.equals(old.getString("id"))) throw failure(source, "invalid recall backpack");
                            String path = source + "/RecallQueues/" + dimensionIndex + "/" + shelfIndex + "/" + ownerIndex + "/" + bagIndex++;
                            refund(uuid, path, convertItem(old, path, result), result);
                            changed = true;
                        }
                        ownerIndex++;
                    }
                    shelfIndex++;
                }
                dimensionIndex++;
            }
            if (!queues.isEmpty()) { data.put("RecallQueues", new ListTag()); changed = true; }
        } else if (source.contains("backpacked_farmhand")) {
            ListTag tasks = optionalList(data, "Tasks", source);
            for (int i = 0; i < tasks.size(); i++) {
                CompoundTag task = compound(tasks.get(i), source);
                String path = source + "/Tasks/" + i;
                refund(uuid(task, "Submitter", path), path,
                        convertItem(optionalCompound(task, "Stack", path), path, result), result);
                changed = true;
            }
            if (changed) data.put("Tasks", new ListTag());
        } else return convertTree(root, source, result);
        if (changed) root.put("data", data);
        return changed;
    }

    private static void refund(UUID owner, String path, CompoundTag stack, Result result) {
        CompoundTag copy = stack.copy();
        copy.remove("Slot");
        ListTag items = new ListTag();
        items.add(copy);
        UUID receipt = UUID.nameUUIDFromBytes(("jem-backpacked-migration:" + path).getBytes(StandardCharsets.UTF_8));
        result.refunds.add(new Refund(owner, receipt, items));
    }

    private static boolean isItem(CompoundTag value) {
        return value.contains("id", Tag.TAG_STRING) && value.get("Count") instanceof NumericTag;
    }

    private static void requireItem(CompoundTag value, String path) throws IOException {
        if (!isItem(value) || value.getInt("Count") <= 0) throw failure(path, "malformed or empty live item stack");
        int type = value.get("Count").getId();
        if (type != Tag.TAG_BYTE && type != Tag.TAG_INT) throw failure(path, "unsupported native item count type");
        if (value.getInt("Count") > 127) throw failure(path, "item count cannot be represented losslessly by native item NBT");
    }

    private static void validateItem(CompoundTag value, String path) throws IOException {
        requireItem(value, path);
        ResourceLocation id = ResourceLocation.tryParse(value.getString("id"));
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id) || id.toString().equals("minecraft:air")) {
            throw failure(path, "unknown live item " + value.getString("id"));
        }
    }

    private static int slot(CompoundTag stack, String path) throws IOException {
        Tag raw = stack.get("Slot");
        if (!(raw instanceof NumericTag number)) throw failure(path, "missing item slot");
        int slot = raw.getId() == Tag.TAG_BYTE ? number.getAsByte() & 255 : number.getAsInt();
        if (slot < 0 || slot > 4095) throw failure(path, "invalid item slot " + slot);
        return slot;
    }

    private static CompoundTag compound(Tag value, String path) throws IOException {
        if (!(value instanceof CompoundTag compound)) throw failure(path, "expected compound");
        return compound;
    }

    private static CompoundTag optionalCompound(CompoundTag owner, String key, String path) throws IOException {
        return owner.contains(key) ? compound(owner.get(key), path + "/" + key) : new CompoundTag();
    }

    private static ListTag optionalList(CompoundTag owner, String key, String path) throws IOException {
        if (!owner.contains(key)) return new ListTag();
        if (!(owner.get(key) instanceof ListTag list) || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            throw failure(path, "invalid compound list " + key);
        }
        return list;
    }

    private static UUID uuid(CompoundTag owner, String key, String path) throws IOException {
        if (!owner.hasUUID(key)) throw failure(path, "missing owner UUID " + key);
        return owner.getUUID(key);
    }

    private static IOException failure(String path, String reason) {
        return new IOException("Backpacked migration at " + path + ": " + reason);
    }
}
