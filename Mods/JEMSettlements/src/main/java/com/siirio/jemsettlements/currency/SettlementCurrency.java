package com.siirio.jemsettlements.currency;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import java.util.UUID;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

/** Settlement-only currency selection. Currency Core's presence never enables the Economy product. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jem_settlements")
public final class SettlementCurrency {
    private static final String MARKER = "JemSettlementEmeraldIntent";
    private SettlementCurrency() { }
    public static boolean coins() { return ModList.get().isLoaded("jem_economy"); }
    public static UUID townAccount(UUID town) { return EconomyApi.townAccount(town); }
    public static long balance(ServerPlayer player) { return coins() ? EconomyApi.balance(player) : 0L; }
    public static long held(ServerPlayer player) {
        if (coins()) return EconomyApi.heldCash(player).value();
        long count = 0L;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot); if (stack.is(Items.EMERALD)) count += stack.getCount();
        }
        return count;
    }
    public static long spendable(ServerPlayer player) { return Math.addExact(balance(player), held(player)); }
    public static long accountBalance(ServerPlayer player, UUID account) {
        if (!SettlementAuthority.active(player) || SettlementAuthority.principal(player).isEmpty()) return 0L;
        return coins() ? EconomyApi.accountBalance(player, account) : EmeraldTreasury.get(player.getServer()).balance(account);
    }
    public static long townBalance(MinecraftServer server, UUID town) {
        if (!SettlementAuthority.active(server)) return 0L;
        return coins() ? EconomyApi.townBalance(server, town) : EmeraldTreasury.get(server).balance(townAccount(town));
    }
    public static TransferResult depositToAccount(ServerPlayer player, UUID destination, long amount, UUID request) {
        if (coins()) return EconomyApi.depositToAccount(player, destination, amount, request);
        return inventoryTransfer(player, destination, amount, request, true);
    }
    public static TransferResult withdrawFromAccount(ServerPlayer player, UUID source, long amount, UUID request) {
        if (coins()) return EconomyApi.withdrawFromAccount(player, source, amount, request);
        return inventoryTransfer(player, source, amount, request, false);
    }
    private static TransferResult result(TransferResult.Status status) { return new TransferResult(status, null, 0L, 0L); }
    private static TransferResult inventoryTransfer(ServerPlayer player, UUID account, long amount, UUID request, boolean deposit) {
        if (!SettlementAuthority.active(player) || account == null || request == null || amount <= 0 || amount > 1_000_000_000L) return result(TransferResult.Status.INVALID);
        var owner = SettlementAuthority.principal(player); if (owner.isEmpty()) return result(TransferResult.Status.UNAUTHENTICATED);
        EmeraldTreasury data = EmeraldTreasury.get(player.getServer());
        recover(player);
        String key = "inventory:" + owner.get() + ":" + request;
        String terms = deposit + ":" + account + ":" + amount;
        var prior = data.prior(key);
        if (prior != null) { data.flush(player.getServer()); return prior.getString("Terms").equals(terms)
                ? new TransferResult(TransferResult.Status.REPLAYED, prior.getUUID("Receipt"), 0L, 0L) : result(TransferResult.Status.INVALID); }
        if (data.locked(account) || data.pending.values().stream().anyMatch(row -> row.getUUID("Player").equals(player.getUUID()))) return result(TransferResult.Status.UNAVAILABLE);
        if (data.full()) return result(TransferResult.Status.UNAVAILABLE);
        long current = data.balance(account);
        if ((deposit && held(player) < amount) || (!deposit && current < amount)) return result(TransferResult.Status.INSUFFICIENT_FUNDS);
        long next;
        try { next = deposit ? Math.addExact(current, amount) : Math.subtractExact(current, amount); }
        catch (ArithmeticException overflow) { return result(TransferResult.Status.INVALID); }
        if (!deposit) {
            long room = 0L; ItemStack emerald = new ItemStack(Items.EMERALD);
            for (ItemStack stack : player.getInventory().items) {
                if (stack.isEmpty()) room += emerald.getMaxStackSize();
                else if (ItemStack.isSameItemSameTags(stack, emerald)) room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
            if (room < amount) return result(TransferResult.Status.UNAVAILABLE);
        }
        net.minecraft.nbt.CompoundTag intent = new net.minecraft.nbt.CompoundTag();
        intent.putString("Key", key); intent.putString("Terms", terms); intent.putUUID("Player", player.getUUID()); intent.putUUID("Actor", owner.get());
        intent.putUUID("Account", account); intent.putLong("Amount", amount); intent.putBoolean("Deposit", deposit);
        intent.putLong("Before", current); intent.putLong("Next", next);
        intent.putUUID("Receipt", UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        data.pending.put(key, intent); data.flush(player.getServer());
        // Server thread owns inventory and account together. Failed insertions restore every slot before any debit.
        var inventory = player.getInventory();
        java.util.List<ItemStack> before = new java.util.ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) before.add(inventory.getItem(slot).copy());
        if (deposit) {
            long remaining = amount;
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inventory.getItem(slot); if (!stack.is(Items.EMERALD)) continue;
                int take = (int) Math.min(remaining, stack.getCount()); stack.shrink(take); remaining -= take;
            }
        } else {
            long remaining = amount;
            while (remaining > 0) {
                int count = (int) Math.min(64L, remaining); ItemStack delivery = new ItemStack(Items.EMERALD, count);
                boolean inserted = inventory.add(delivery);
                if (!inserted || !delivery.isEmpty()) {
                    for (int slot = 0; slot < before.size(); slot++) inventory.setItem(slot, before.get(slot));
                    inventory.setChanged(); player.containerMenu.broadcastChanges(); data.pending.remove(key); data.flush(player.getServer()); return result(TransferResult.Status.UNAVAILABLE);
                }
                remaining -= count;
            }
        }
        player.getPersistentData().put(MARKER, intent.copy()); inventory.setChanged();
        try { persistPlayer(player); }
        catch (RuntimeException failure) {
            // Native save may already have replaced the file. Never undo a durable mutation.
            var disk = savedPlayer(player);
            if (!disk.getCompound("ForgeData").getCompound(MARKER).equals(intent)) {
                for (int slot = 0; slot < before.size(); slot++) inventory.setItem(slot, before.get(slot));
                player.getPersistentData().remove(MARKER); inventory.setChanged();
            }
            throw failure;
        }
        commit(player.getServer(), data, intent);
        player.containerMenu.broadcastChanges();
        return new TransferResult(TransferResult.Status.COMMITTED, intent.getUUID("Receipt"), deposit ? held(player) : next, deposit ? next : held(player));
    }
    private static net.minecraft.nbt.CompoundTag savedPlayer(ServerPlayer player) {
        var server = player.getServer();
        // PlayerList.load prefers WorldData's loaded Player tag for the integrated owner.
        // IntegratedPlayerList.save updates that tag in memory before also writing playerdata.
        // Only level.dat, after saveEverything, durably commits the owner's restart inventory.
        if (server.isSingleplayerOwner(player.getGameProfile())) {
            var levelFile = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("level.dat").toFile();
            try {
                if (levelFile.exists()) {
                    var level = net.minecraft.nbt.NbtIo.readCompressed(levelFile).getCompound("Data");
                    if (level.contains("Player", net.minecraft.nbt.Tag.TAG_COMPOUND)) return level.getCompound("Player");
                }
            } catch (java.io.IOException failure) { throw new IllegalStateException("Emerald integrated owner storage unavailable", failure); }
        }
        // This is also native PlayerList.load's fallback when no owner Player tag exists yet.
        var file = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID() + ".dat").toFile();
        try { return file.exists() ? net.minecraft.nbt.NbtIo.readCompressed(file) : new net.minecraft.nbt.CompoundTag(); }
        catch (java.io.IOException failure) { throw new IllegalStateException("Emerald player storage unavailable", failure); }
    }
    private static void persistPlayer(ServerPlayer player) {
        var expected = player.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        ((com.siirio.jemsettlements.mixin.PlayerSaveAccess) player.getServer().getPlayerList()).jem$settlementsSave(player);
        if (player.getServer().isSingleplayer()) player.getServer().saveEverything(false, true, true);
        var actual = savedPlayer(player);
        if (!expected.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND).equals(actual.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND))
                || !expected.getCompound("ForgeData").getCompound(MARKER).equals(actual.getCompound("ForgeData").getCompound(MARKER)))
            throw new IllegalStateException("Emerald inventory and intent marker were not saved together");
    }
    private static void commit(MinecraftServer server, EmeraldTreasury data, net.minecraft.nbt.CompoundTag intent) {
        String key = intent.getString("Key"); UUID account = intent.getUUID("Account");
        if (data.prior(key) == null) {
            if (data.balance(account) != intent.getLong("Before")) throw new IllegalStateException("Pending emerald account changed");
            data.accounts.put(account, intent.getLong("Next")); boolean deposit = intent.getBoolean("Deposit");
            data.record(key, intent.getString("Terms"), deposit ? intent.getUUID("Actor") : account,
                    deposit ? account : intent.getUUID("Actor"), intent.getLong("Amount"));
        }
        data.pending.remove(key); data.flush(server);
    }
    public static void recover(ServerPlayer player) {
        EmeraldTreasury data = EmeraldTreasury.get(player.getServer());
        var intents = data.pending.values().stream().filter(row -> row.getUUID("Player").equals(player.getUUID())).map(net.minecraft.nbt.CompoundTag::copy).toList();
        if (intents.isEmpty()) { if (data.isDirty()) data.flush(player.getServer()); return; }
        var durableMarker = savedPlayer(player).getCompound("ForgeData").getCompound(MARKER);
        for (var intent : intents) {
            if (intent.equals(durableMarker)) {
                if (!intent.equals(player.getPersistentData().getCompound(MARKER)))
                    throw new IllegalStateException("Loaded player does not match the durable emerald intent");
                commit(player.getServer(), data, intent);
            }
            else { data.pending.remove(intent.getString("Key")); data.flush(player.getServer()); }
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SettlementAuthority.active(player)) recover(player);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(MARKER))
            event.getEntity().getPersistentData().put(MARKER, event.getOriginal().getPersistentData().getCompound(MARKER).copy());
    }
    public static boolean canDelete(ServerPlayer player, UUID town) {
        recover(player); var data = EmeraldTreasury.get(player.getServer()); UUID account = townAccount(town);
        if (data.locked(account)) return false;
        return coins() ? data.balance(account) == 0L
                : com.siirio.jemeconomy.ledger.EconomySavedData.get(player.getServer()).balance(account) == 0L;
    }
    public static TransferResult settleTownPayroll(MinecraftServer server, UUID town, long amount, long period) {
        if (coins()) return EconomyApi.settleTownPayroll(server, town, amount, period);
        return debit(server, townAccount(town), amount, "townPayroll:" + town + ":" + period);
    }
    public static TransferResult settleEmployerPayroll(MinecraftServer server, UUID owner, long amount, long period) {
        if (coins()) return EconomyApi.settleEmployerPayroll(server, owner, amount, period);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) if (SettlementAuthority.principal(player).filter(owner::equals).isPresent())
            return depositToAccount(player, com.siirio.jemeconomy.ledger.SystemAccount.CENTRAL_RESERVE.id(), amount,
                    UUID.nameUUIDFromBytes(("emeraldEmployerPayroll:" + owner + ":" + period).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return result(TransferResult.Status.INSUFFICIENT_FUNDS);
    }
    private static TransferResult debit(MinecraftServer server, UUID account, long amount, String key) {
        if (!SettlementAuthority.active(server) || amount <= 0L) return result(TransferResult.Status.INVALID);
        EmeraldTreasury data = EmeraldTreasury.get(server); String terms = account + ":" + amount;
        var prior = data.prior(key);
        if (prior != null) { data.flush(server); return prior.getString("Terms").equals(terms)
                ? new TransferResult(TransferResult.Status.REPLAYED, prior.getUUID("Receipt"), 0L, 0L) : result(TransferResult.Status.INVALID); }
        if (data.full() || data.locked(account)) return result(TransferResult.Status.UNAVAILABLE);
        long current = data.balance(account); if (current < amount) return result(TransferResult.Status.INSUFFICIENT_FUNDS);
        data.accounts.put(account, current - amount);
        UUID receipt = data.record(key, terms, account, com.siirio.jemeconomy.ledger.SystemAccount.CENTRAL_RESERVE.id(), amount);
        data.flush(server);
        return new TransferResult(TransferResult.Status.COMMITTED, receipt, current - amount, 0L);
    }
    public static ListTag emeraldHistory(MinecraftServer server, UUID town) { return EmeraldTreasury.get(server).history(townAccount(town)); }
    /** Server-authored daily NPC income. Coins transfer existing funds; standalone emeralds are vanilla NPC income. */
    public static TransferResult creditDailyTax(MinecraftServer server, UUID town, long amount, long day) {
        if (!SettlementAuthority.active(server) || town == null || amount <= 0 || day < 0) return result(TransferResult.Status.INVALID);
        String key = "settlementDailyTax:" + town + ":" + day;
        if (coins()) {
            var ledger = com.siirio.jemeconomy.ledger.EconomySavedData.get(server);
            var transfer = ledger.hasReceipt(key)
                    ? ledger.transfer(com.siirio.jemeconomy.ledger.SystemAccount.OPERATIONS.id(), townAccount(town), amount, key)
                    : ledger.issueSettlementIncome(town, townAccount(town), amount, day);
            if (transfer.status() == TransferResult.Status.COMMITTED || transfer.status() == TransferResult.Status.REPLAYED)
                com.siirio.jemsettlements.payroll.TaxDurability.save(server, ledger, "jem_economy");
            return transfer;
        }
        var data = EmeraldTreasury.get(server); UUID account = townAccount(town); String terms = account + ":" + amount;
        var prior = data.prior(key);
        if (prior != null) {
            data.flush(server);
            return prior.getString("Terms").equals(terms)
                    ? new TransferResult(TransferResult.Status.REPLAYED, prior.getUUID("Receipt"), 0L, data.balance(account))
                    : result(TransferResult.Status.INVALID);
        }
        if (data.full() || data.locked(account)) return result(TransferResult.Status.UNAVAILABLE);
        long next;
        try { next = Math.addExact(data.balance(account), amount); }
        catch (ArithmeticException overflow) { return result(TransferResult.Status.INVALID); }
        data.accounts.put(account, next);
        UUID receipt = data.record(key, terms, UUID.nameUUIDFromBytes("jem:settlements:npc_daily_tax".getBytes(java.nio.charset.StandardCharsets.UTF_8)), account, amount);
        data.flush(server);
        return new TransferResult(TransferResult.Status.COMMITTED, receipt, 0L, next);
    }
    public static net.minecraft.network.chat.Component format(long amount) {
        return coins() ? com.siirio.jemeconomy.physical.CoinFormatter.format(amount)
                : net.minecraft.network.chat.Component.translatable("jem.settlements.currency.emerald_amount", amount);
    }
}
