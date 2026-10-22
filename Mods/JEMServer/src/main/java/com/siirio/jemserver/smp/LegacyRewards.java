package com.siirio.jemserver.smp;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class LegacyRewards {
    private static final String DEPOSIT_RECEIPT = "jem_smp_deposit_receipt";
    private static final String DELIVERY_RECEIPT = "jem_smp_delivery_receipt";

    private static void enqueue(ListTag pending, ItemStack stack) {
        for (int index = 0; index < pending.size() && !stack.isEmpty(); index++) {
            ItemStack existing = ItemStack.of(pending.getCompound(index));
            if (!ItemStack.isSameItemSameTags(existing, stack)) continue;
            int count =
                    Math.min(
                            stack.getCount(),
                            Math.max(0, existing.getMaxStackSize() - existing.getCount()));
            if (count > 0) {
                existing.grow(count);
                stack.shrink(count);
                pending.set(index, existing.save(new CompoundTag()));
            }
        }
        while (!stack.isEmpty()) {
            SmpRecords.require(
                    pending.size() < SmpConfig.CONTRACT_DELIVERIES.get(), "delivery_queue_full");
            int count = Math.min(stack.getCount(), stack.getMaxStackSize());
            var next = stack.copy();
            next.setCount(count);
            pending.add(next.save(new CompoundTag()));
            stack.shrink(count);
        }
    }

    public static void migrate(ServerPlayer player) {
        recover(player);
        var data = SmpData.get(player.server);
        var profile = Profiles.get(player.server, player.getUUID());
        if (!DomainFeatures.loaded("jem_rewards")) {
            if (profile.contains("rewardMigration", Tag.TAG_COMPOUND) || !profile.getList("deliveries", Tag.TAG_COMPOUND).isEmpty()) {
                if (!profile.getString("rewardAvailability").equals("UNAVAILABLE_MISSING_REWARD_OWNER")) {
                    profile.putString("rewardAvailability", "UNAVAILABLE_MISSING_REWARD_OWNER");
                    data.changed(profile); data.flush(player.server);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.escrow_recovery_required"));
                }
            }
            return;
        }
        if (profile.contains("rewardAvailability")) { profile.remove("rewardAvailability"); data.changed(profile); data.flush(player.server); }
        while (profile.contains("rewardMigration", Tag.TAG_COMPOUND)
                || !profile.getList("deliveries", Tag.TAG_COMPOUND).isEmpty()) {
            if (!profile.contains("rewardMigration", Tag.TAG_COMPOUND)) {
                var journal = new CompoundTag();
                journal.putUUID("id", UUID.nameUUIDFromBytes(("jem_smp_deliveries:" + player.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                journal.put("items", profile.getList("deliveries", Tag.TAG_COMPOUND).copy());
                profile.put("rewardMigration", journal);
                profile.remove("deliveries");
                data.changed(profile);
                data.flush(player.server);
            }
            var journal = profile.getCompound("rewardMigration");
            if (!journal.getBoolean("committed")) {
                RewardImports.importLegacy(
                        player, journal.getUUID("id"), journal.getList("items", Tag.TAG_COMPOUND));
                journal.putBoolean("committed", true);
                data.changed(profile);
            }
            data.flush(player.server);
            RewardImports.finishLegacyImport(
                    player, journal.getUUID("id"));
            profile.remove("rewardMigration");
            data.changed(profile);
            data.flush(player.server);
        }
    }
    public static void recover(ServerPlayer player) {
        var data = SmpData.get(player.server);
        var profile = Profiles.get(player.server, player.getUUID());
        if (player.getPersistentData().contains(DELIVERY_RECEIPT, Tag.TAG_COMPOUND)) {
            finishDelivery(
                    player, profile, player.getPersistentData().getCompound(DELIVERY_RECEIPT));
        } else if (profile.contains("deliveryClaim", Tag.TAG_COMPOUND)) {
            profile.remove("deliveryClaim");
            data.changed(profile);
            data.flush(player.server);
        }
        var receipt = player.getPersistentData();
        var rows =
                data.all("contracts").stream()
                        .filter(
                                row ->
                                        row.hasUUID("owner")
                                                && row.getUUID("owner").equals(player.getUUID())
                                                && row.contains("escrow", Tag.TAG_LIST)
                                                && !row.contains("legacyDisposition"))
                        .limit(SmpConfig.DELIVERY_BATCH.get())
                        .toList();
        for (var row : rows) {
            boolean deposited =
                    receipt.hasUUID(DEPOSIT_RECEIPT)
                            && receipt.getUUID(DEPOSIT_RECEIPT).equals(row.getUUID("id"));
            if (row.getString("state").startsWith("FUNDING") && !deposited) {
                row.putString("legacyDisposition", "NOT_DEBITED");
            } else {
                UUID migrationId = UUID.nameUUIDFromBytes(("jem_smp_contract:" + row.getUUID("id")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                com.siirio.jemrewards.PendingRewardContainer.importOffline(player.server, player.getUUID(), migrationId,
                        row.getList("escrow", Tag.TAG_COMPOUND));
                row.remove("escrow");
                row.putString("legacyDisposition", "RETURNED_TO_OWNER");
            }
            row.putString("state", "CANCELLED");
            data.changed(row);
            data.flush(player.server);
        }
        if (receipt.hasUUID(DEPOSIT_RECEIPT)) {
            var row = data.find("contracts", receipt.getUUID(DEPOSIT_RECEIPT));
            SmpRecords.require(row != null, "escrow_recovery_required");
            if (!row.getString("state").startsWith("FUNDING")
                    || row.contains("legacyDisposition")) {
                data.flush(player.server);
                receipt.remove(DEPOSIT_RECEIPT);
                persistPlayer(player);
            }
        }
    }

    private static void finishDelivery(
            ServerPlayer player, CompoundTag profile, CompoundTag receipt) {
        var data = SmpData.get(player.server);
        if (profile.contains("deliveryClaim", Tag.TAG_COMPOUND)) {
            var journal = profile.getCompound("deliveryClaim");
            SmpRecords.require(
                    receipt.hasUUID("id") && receipt.getUUID("id").equals(journal.getUUID("id")),
                    "escrow_recovery_required");
            persistPlayer(player);
            profile.put("deliveries", receipt.getList("remaining", Tag.TAG_COMPOUND).copy());
            profile.remove("deliveryClaim");
            data.changed(profile);
        }
        data.flush(player.server);
        player.getPersistentData().remove(DELIVERY_RECEIPT);
        persistPlayer(player);
    }

    private static void persistPlayer(ServerPlayer player) {
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        ((com.siirio.jemserver.mixin.PlayerSaveAccess) player.server.getPlayerList())
                .jem$save(player);
        if (player.server.isSingleplayer()) player.server.saveEverything(false, true, true);
        var file =
                player.server
                        .getWorldPath(
                                net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR)
                        .resolve(player.getUUID() + ".dat")
                        .toFile();
        try {
            CompoundTag saved = NbtIo.readCompressed(file);
            boolean matches =
                    expected.getList("Inventory", Tag.TAG_COMPOUND)
                            .equals(saved.getList("Inventory", Tag.TAG_COMPOUND));
            for (String key : List.of(DEPOSIT_RECEIPT, DELIVERY_RECEIPT))
                matches &=
                        Objects.equals(
                                expected.getCompound("ForgeData").get(key),
                                saved.getCompound("ForgeData").get(key));
            if (!matches)
                throw new java.io.IOException("Inventory or escrow receipt not persisted");
        } catch (java.io.IOException failure) {
            com.mojang.logging.LogUtils.getLogger()
                    .error("Could not verify escrow player save for {}", player.getUUID(), failure);
            throw new com.siirio.jemserver.smp.SmpActionFailure("storage_unavailable");
        }
    }

    private static final class RewardImports {
        static void importLegacy(ServerPlayer player, UUID id, ListTag items) {
            com.siirio.jemrewards.PendingRewardContainer.importLegacy(player, id, items);
        }
        static void finishLegacyImport(ServerPlayer player, UUID id) {
            com.siirio.jemrewards.PendingRewardContainer.finishLegacyImport(player, id);
        }
    }

    private LegacyRewards() {}
}
