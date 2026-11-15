package com.siirio.jemrewards;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;

/** One modpack-level starter entitlement for integrated and dedicated logical servers. */
final class ModpackStarterKit {
    private static final String BASE_SOURCE = "starter_kit:";
    private static final String EXPLORATION_SOURCE = "starter_kit_exploration_v1:";
    private static final String LEGACY_DATA = "jem_starter_kits.dat";
    private static final String LEGACY_RECEIPT = "jem_server:starter_kit";
    private static final String TITLE = "jem_rewards.starter_kit";

    private ModpackStarterKit() {}

    static void deliver(ServerPlayer player) {
        UUID recipient = player.getUUID();
        String baseSource = BASE_SOURCE + recipient;
        String explorationSource = EXPLORATION_SOURCE + recipient;
        boolean baseIssued = PendingRewardContainer.hasIssued(player.server, recipient, baseSource);
        List<ItemStack> legacy = baseIssued ? null : legacyRemaining(player);

        if (!PendingRewardContainer.hasIssued(player.server, recipient, explorationSource)) {
            boolean existingRecipient = baseIssued || legacy != null;
            PendingRewardContainer.enqueue(player.server, recipient, explorationSource, TITLE,
                    existingRecipient ? explorationItems() : List.of());
        }
        if (!baseIssued) {
            PendingRewardContainer.enqueue(player.server, recipient, baseSource, TITLE,
                    legacy == null ? completeKit() : legacy);
        }
    }

    private static List<ItemStack> completeKit() {
        List<ItemStack> items = new ArrayList<>(List.of(
                new ItemStack(Items.COOKED_BEEF, 64),
                new ItemStack(Items.IRON_INGOT, 10),
                new ItemStack(Items.LEATHER_HELMET),
                new ItemStack(Items.LEATHER_CHESTPLATE),
                new ItemStack(Items.LEATHER_LEGGINGS),
                new ItemStack(Items.LEATHER_BOOTS),
                nativeItem("sophisticatedbackpacks:backpack"),
                nativeItem("naturescompass:naturescompass")));
        items.addAll(explorationItems());
        return List.copyOf(items);
    }

    private static List<ItemStack> explorationItems() {
        return List.of(new ItemStack(Items.SPYGLASS), nativeItem("paraglider:paraglider"));
    }

    private static ItemStack nativeItem(String id) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) throw new IllegalStateException("Missing starter item " + id);
        return new ItemStack(item);
    }

    /** Returns null when no old server-owned entitlement exists. */
    private static List<ItemStack> legacyRemaining(ServerPlayer player) {
        Path file = player.server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(LEGACY_DATA);
        if (!Files.exists(file)) return null;
        try {
            CompoundTag accounts = NbtIo.readCompressed(file.toFile()).getCompound("data").getCompound("Accounts");
            CompoundTag row = accounts.getCompound(player.getUUID().toString());
            if (row.isEmpty()) return null;
            if (row.getBoolean("Complete")) return List.of();

            ListTag remaining = row.getList("Remaining", Tag.TAG_COMPOUND);
            CompoundTag pending = row.getCompound("Pending");
            if (!pending.isEmpty() && matchesLegacyReceipt(player, pending)) {
                remaining = pending.getList("Remaining", Tag.TAG_COMPOUND);
            } else if (row.contains("RewardTransfer", Tag.TAG_COMPOUND)) {
                remaining = row.getCompound("RewardTransfer").getList("Items", Tag.TAG_COMPOUND);
            }
            return loadItems(remaining);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read the legacy starter entitlement", failure);
        }
    }

    private static boolean matchesLegacyReceipt(ServerPlayer player, CompoundTag pending) {
        CompoundTag receipt = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                .getCompound(LEGACY_RECEIPT);
        return receipt.hasUUID("Account") && receipt.hasUUID("Operation")
                && receipt.getUUID("Account").equals(player.getUUID())
                && receipt.getUUID("Operation").equals(pending.getUUID("Operation"));
    }

    private static List<ItemStack> loadItems(ListTag saved) {
        List<ItemStack> items = new ArrayList<>();
        for (Tag value : saved) {
            CompoundTag item = ((CompoundTag) value).copy();
            if (item.getString("id").equals("backpacked:backpack")
                    && (!item.contains("tag") || item.getCompound("tag").isEmpty())) {
                item.putString("id", "sophisticatedbackpacks:backpack");
            }
            ItemStack stack = ItemStack.of(item);
            if (stack.isEmpty()) throw new IllegalStateException("Legacy starter kit contains an unavailable item");
            items.add(stack);
        }
        return List.copyOf(items);
    }
}
