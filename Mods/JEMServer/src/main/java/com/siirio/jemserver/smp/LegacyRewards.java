package com.siirio.jemserver.smp;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class LegacyRewards {
    private static final String DEPOSIT_RECEIPT = "jem_smp_deposit_receipt";
    private static final String DELIVERY_RECEIPT = "jem_smp_delivery_receipt";

    public static void migrate(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> migrateOwned(player));
    }

    private static void migrateOwned(ServerPlayer player) {
        recover(player);
        var data = SmpData.get(player.server);
        var profile = Profiles.get(player.server, player.getUUID());
        if (!DomainFeatures.loaded("jem_rewards")) {
            if (profile.rewardMigration() != null || !profile.deliveries().isEmpty()) {
                if (!profile.rewardAvailability().equals("UNAVAILABLE_MISSING_REWARD_OWNER")) {
                    profile.rewardAvailability("UNAVAILABLE_MISSING_REWARD_OWNER");
                     data.flush(player.server);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.escrow_recovery_required"));
                }
            }
            return;
        }
        if (profile.hasRewardAvailability()) { profile.clearRewardAvailability();  data.flush(player.server); }
        while (profile.rewardMigration() != null
                || !profile.deliveries().isEmpty()) {
            if (profile.rewardMigration() == null) {
                UUID id = UUID.nameUUIDFromBytes(("jem_smp_deliveries:" + player.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                profile.rewardMigration(new ProfileState.RewardMigration(id, profile.deliveries(), false));
                profile.clearDeliveries();
                data.flush(player.server);
            }
            var journal = profile.rewardMigration();
            if (!journal.committed()) {
                RewardImports.importLegacy(
                        player, journal.id(), journal.items().toNbt());
                profile.rewardMigration(new ProfileState.RewardMigration(journal.id(), journal.items(), true));
                
            }
            data.flush(player.server);
            RewardImports.finishLegacyImport(
                    player, journal.id());
            profile.clearRewardMigration();
            
            data.flush(player.server);
        }
    }
    public static void recover(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> recoverOwned(player));
    }

    private static void recoverOwned(ServerPlayer player) {
        var data = SmpData.get(player.server);
        var profile = Profiles.get(player.server, player.getUUID());
        if (player.getPersistentData().contains(DELIVERY_RECEIPT, Tag.TAG_COMPOUND)) {
            finishDelivery(
                    player, profile, player.getPersistentData().getCompound(DELIVERY_RECEIPT));
        } else if (profile.deliveryClaim() != null) {
            profile.clearDeliveryClaim();
            
            data.flush(player.server);
        }
        var receipt = player.getPersistentData();
        var rows =
                data.contracts().currentRows().stream()
                        .filter(
                                row ->
                                        row.hasOwner()
                                                && row.owner().equals(player.getUUID())
                                                && row.hasEscrow()
                                                && !row.hasLegacyDisposition())
                        .limit(SmpConfig.DELIVERY_BATCH.get())
                        .toList();
        for (var row : rows) {
            boolean deposited =
                    receipt.hasUUID(DEPOSIT_RECEIPT)
                            && receipt.getUUID(DEPOSIT_RECEIPT).equals(row.id());
            if (row.state().startsWith("FUNDING") && !deposited) {
                row.legacyDisposition("NOT_DEBITED");
            } else {
                UUID migrationId = UUID.nameUUIDFromBytes(("jem_smp_contract:" + row.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                com.siirio.jemrewards.PendingRewardContainer.importOffline(player.server, player.getUUID(), migrationId,
                        row.escrow());
                row.clearEscrow();
                row.legacyDisposition("RETURNED_TO_OWNER");
            }
            row.state("CANCELLED");
            
            data.flush(player.server);
        }
        if (receipt.hasUUID(DEPOSIT_RECEIPT)) {
            var row = data.contracts().current(receipt.getUUID(DEPOSIT_RECEIPT));
            SmpRecords.require(row != null, "escrow_recovery_required");
            if (!row.state().startsWith("FUNDING")
                    || row.hasLegacyDisposition()) {
                data.flush(player.server);
                receipt.remove(DEPOSIT_RECEIPT);
                persistPlayer(player);
            }
        }
    }

    private static void finishDelivery(
            ServerPlayer player, ProfileState profile, CompoundTag receipt) {
        var data = SmpData.get(player.server);
        data.edit(() -> finishDeliveryOwned(player, data.profiles().bind(profile), receipt));
    }

    private static void finishDeliveryOwned(
            ServerPlayer player, ProfileState profile, CompoundTag receipt) {
        var data = SmpData.get(player.server);
        if (profile.deliveryClaim() != null) {
            var journal = profile.deliveryClaim();
            SmpRecords.require(
                    receipt.hasUUID("id") && receipt.getUUID("id").equals(journal.id()),
                    "escrow_recovery_required");
            persistPlayer(player);
            profile.deliveries(NativeItems.fromNbt(receipt.getList("remaining", Tag.TAG_COMPOUND)));
            profile.clearDeliveryClaim();
            
        }
        data.flush(player.server);
        player.getPersistentData().remove(DELIVERY_RECEIPT);
        persistPlayer(player);
    }

    private static void persistPlayer(ServerPlayer player) {
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        try {
            CompoundTag saved = NativePlayerFiles.saveAndVerifyInventory(player, true);
            boolean matches = true;
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
