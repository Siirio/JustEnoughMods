package com.siirio.jemsecurity;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ItemProvenance {
    private static final String ROOT = "jem_security";
    private static final String ID = "provenance_id";
    private static final Map<UUID, Map<UUID, Snapshot>> LAST = new HashMap<>();

    private ItemProvenance() { }

    public static void creativeSlot(ServerPlayer player, int slot, ItemStack incoming) {
        if (!SecurityRuntime.active(player.server)) return;
        ItemStack previous = player.inventoryMenu.isValidSlotIndex(slot) ? player.inventoryMenu.getSlot(slot).getItem() : ItemStack.EMPTY;
        if (incoming.isEmpty()) {
            JsonObject details = stackDetails(previous);
            details.addProperty("slot", slot);
            SecurityAuditLog.record(player, "creative_slot_cleared", details);
            return;
        }
        var tag = incoming.getOrCreateTagElement(ROOT);
        if (!tag.hasUUID(ID)) tag.putUUID(ID, UUID.randomUUID());
        tag.putUUID("created_by", player.getUUID());
        tag.putString("created_at", Instant.now().toString());
        JsonObject details = stackDetails(incoming);
        details.addProperty("slot", slot);
        details.addProperty("previousItem", itemId(previous));
        SecurityAuditLog.record(player, slot < 0 ? "creative_item_dropped" : "creative_item_created", details);
    }

    public static void scan(MinecraftServer server) {
        if (!SecurityRuntime.active(server)) return;
        Map<UUID, Map<UUID, Snapshot>> current = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<UUID, Snapshot> playerItems = new HashMap<>();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                UUID id = provenance(stack);
                if (id == null) continue;
                Snapshot existing = playerItems.get(id);
                int count = stack.getCount() + (existing == null ? 0 : existing.count());
                playerItems.put(id, new Snapshot(itemId(stack), count, slot));
            }
            current.put(player.getUUID(), playerItems);
        }
        for (var priorPlayer : LAST.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(priorPlayer.getKey());
            if (player == null) continue;
            Map<UUID, Snapshot> now = current.getOrDefault(priorPlayer.getKey(), Map.of());
            for (var prior : priorPlayer.getValue().entrySet()) {
                Snapshot next = now.get(prior.getKey());
                if (next == null) {
                    JsonObject details = snapshotDetails(prior.getKey(), prior.getValue());
                    SecurityAuditLog.record(player, "creative_item_left_inventory", details);
                } else if (next.count() < prior.getValue().count()) {
                    JsonObject details = snapshotDetails(prior.getKey(), next);
                    details.addProperty("previousCount", prior.getValue().count());
                    SecurityAuditLog.record(player, "creative_item_count_decreased", details);
                }
            }
        }
        LAST.clear();
        LAST.putAll(current);
    }

    public static UUID provenance(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        var root = stack.getTagElement(ROOT);
        return root != null && root.hasUUID(ID) ? root.getUUID(ID) : null;
    }

    public static JsonObject stackDetails(ItemStack stack) {
        JsonObject details = new JsonObject();
        details.addProperty("item", itemId(stack));
        details.addProperty("count", stack == null ? 0 : stack.getCount());
        UUID id = provenance(stack);
        if (id != null) details.addProperty("provenanceId", id.toString());
        if (stack != null && stack.hasTag()) details.addProperty("nbt", bounded(stack.getTag().toString(), 2048));
        return details;
    }

    public static void clear() {
        LAST.clear();
    }

    private static JsonObject snapshotDetails(UUID id, Snapshot snapshot) {
        JsonObject details = new JsonObject();
        details.addProperty("provenanceId", id.toString());
        details.addProperty("item", snapshot.item());
        details.addProperty("count", snapshot.count());
        details.addProperty("lastSlot", snapshot.slot());
        return details;
    }

    private static String itemId(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String bounded(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private record Snapshot(String item, int count, int slot) { }
}
