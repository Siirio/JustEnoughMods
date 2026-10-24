package com.siirio.jemserver.smp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

final class StarterKitDelivery {
    static final String RECEIPT = "jem_server:starter_kit";
    private StarterKitDelivery() {}

    static void deliver(ServerPlayer player, UUID account) throws IOException {
        var entitlements = StarterEntitlements.get(player.server);
        if (!entitlements.exists(account)) entitlements.ensure(player.server, account, kit());
        CompoundTag pending = entitlements.pending(account);
        if (!pending.isEmpty()) reconcile(player, account, pending, entitlements);
        if (entitlements.complete(player.server, account)) return;
        CompoundTag transfer = entitlements.prepareRewardTransfer(player.server, account, player.getUUID());
        List<ItemStack> remaining = new ArrayList<>();
        for (Tag value : transfer.getList("Items", Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.of((CompoundTag) value);
            if (stack.isEmpty()) throw new IOException("Starter kit contains an unavailable saved item");
            remaining.add(stack);
        }
        com.siirio.jemrewards.PendingRewardContainer.enqueue(player.server, transfer.getUUID("Player"),
                "starter_kit:" + account, "jem_rewards.starter_kit", remaining);
        entitlements.completeTransfer(player.server, account);
    }

    private static void reconcile(ServerPlayer player, UUID account, CompoundTag pending,
                                  StarterEntitlements entitlements) throws IOException {
        UUID operation = pending.getUUID("Operation");
        UUID recipient = pending.getUUID("Player");
        if (matches(persisted(player).getCompound(RECEIPT), account, operation)) {
            CompoundTag saved = NativePlayerFiles.saveAndVerifyInventory(player, false);
            if (!matches(saved.getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG)
                    .getCompound(RECEIPT), account, operation))
                throw new IOException("Starter kit inventory receipt was not persisted");
            entitlements.reconcileLegacy(player.server, account, true);
            return;
        }
        CompoundTag saved = NativePlayerFiles.read(player.server, recipient);
        if (matches(saved.getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG)
                .getCompound(RECEIPT), account, operation)) {
            entitlements.reconcileLegacy(player.server, account, true);
            return;
        }
        if (!recipient.equals(player.getUUID()) && player.server.getPlayerList().getPlayer(recipient) != null)
            throw new IOException("Starter kit operation still belongs to another connected player");
        entitlements.reconcileLegacy(player.server, account, false);
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
                nativeItem("sophisticatedbackpacks:backpack"), nativeItem("naturescompass:naturescompass"));
        ListTag result = new ListTag();
        for (ItemStack item : items) result.add(item.save(new CompoundTag()));
        return result;
    }

    private static ItemStack nativeItem(String id) throws IOException {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) throw new IOException("Missing starter item " + id);
        return new ItemStack(item);
    }

}
