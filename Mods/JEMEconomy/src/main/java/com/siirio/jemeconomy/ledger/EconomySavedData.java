package com.siirio.jemeconomy.ledger;

import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemeconomy.physical.CoinLot;
import com.siirio.jemeconomy.physical.PhysicalCoinOperation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class EconomySavedData extends SavedData {
    private static final String NAME = "jem_economy";
    private static final int SCHEMA = 2;
    private static final int MAX_RECEIPTS = 50_000;
    private static final int MAX_OPERATIONS = 50_000;
    private static final int MAX_LOTS = 100_000;
    private static final long INITIAL_SUPPLY = 100_000L;

    private final Map<UUID, Long> balances = new LinkedHashMap<>();
    private final LinkedHashMap<String, UUID> receipts = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, CoinLot> lots = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, PhysicalCoinOperation> physicalOperations = new LinkedHashMap<>();
    private boolean frozen;

    public EconomySavedData() {
        for (SystemAccount account : SystemAccount.values()) balances.put(account.id(), account.initialBalance());
    }

    public static EconomySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EconomySavedData::load, EconomySavedData::new, NAME);
    }

    public synchronized long balance(UUID id) {
        return id == null ? 0L : balances.getOrDefault(id, 0L);
    }

    public synchronized boolean frozen() {
        return frozen;
    }

    public synchronized long physicalOutstanding() {
        return balance(SystemAccount.PHYSICAL_OUTSTANDING.id());
    }

    public synchronized TransferResult transfer(UUID source, UUID destination, long amount, String idempotencyKey) {
        if (frozen) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, balance(source), balance(destination));
        if (source == null || destination == null || source.equals(destination) || amount <= 0L ||
                idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 160) {
            return new TransferResult(TransferResult.Status.INVALID, null, balance(source), balance(destination));
        }
        UUID prior = receipts.get(idempotencyKey);
        if (prior != null) return new TransferResult(TransferResult.Status.REPLAYED, prior, balance(source), balance(destination));
        long sourceBalance = balance(source);
        if (sourceBalance < amount) return new TransferResult(TransferResult.Status.INSUFFICIENT_FUNDS, null, sourceBalance, balance(destination));
        long destinationBalance;
        try { destinationBalance = Math.addExact(balance(destination), amount); }
        catch (ArithmeticException overflow) { return new TransferResult(TransferResult.Status.INVALID, null, sourceBalance, balance(destination)); }
        balances.put(source, sourceBalance - amount);
        balances.put(destination, destinationBalance);
        UUID receipt = UUID.nameUUIDFromBytes(("jem:receipt:" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
        receipts.put(idempotencyKey, receipt);
        trimReceipts();
        if (!conservationValid()) frozen = true;
        setDirty();
        return frozen
                ? new TransferResult(TransferResult.Status.UNAVAILABLE, receipt, sourceBalance - amount, destinationBalance)
                : new TransferResult(TransferResult.Status.COMMITTED, receipt, sourceBalance - amount, destinationBalance);
    }

    public synchronized WithdrawLedgerResult prepareWithdraw(UUID accountId, long amount, UUID operationId) {
        if (frozen || accountId == null || operationId == null || amount <= 0L || amount > 1_000_000_000L) {
            return new WithdrawLedgerResult(PhysicalStatus.INVALID, operationId, 0L, List.of());
        }
        PhysicalCoinOperation replay = physicalOperations.get(operationId);
        if (replay != null) {
            List<CoinLot> replayLots = replay.lotQuantities().keySet().stream().map(lots::get).filter(Objects::nonNull).toList();
            return new WithdrawLedgerResult(replay.phase() == PhysicalCoinOperation.Phase.QUARANTINED ? PhysicalStatus.QUARANTINED : PhysicalStatus.REPLAYED,
                    operationId, replay.value(), replayLots);
        }
        TransferResult debit = transfer(accountId, SystemAccount.PHYSICAL_OUTSTANDING.id(), amount, "withdraw:" + operationId);
        if (debit.status() == TransferResult.Status.INSUFFICIENT_FUNDS) {
            return new WithdrawLedgerResult(PhysicalStatus.INSUFFICIENT_FUNDS, operationId, 0L, List.of());
        }
        if (debit.status() != TransferResult.Status.COMMITTED) {
            return new WithdrawLedgerResult(PhysicalStatus.INVALID, operationId, 0L, List.of());
        }
        long remainder = amount;
        List<CoinLot> created = new ArrayList<>();
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        int sequence = 0;
        for (CoinDenomination denomination : CoinDenomination.descending()) {
            long units = remainder / denomination.value();
            remainder %= denomination.value();
            while (units > 0L) {
                int count = (int) Math.min(64L, units);
                UUID lotId = UUID.nameUUIDFromBytes((operationId + ":" + denomination.id() + ":" + sequence++).getBytes(StandardCharsets.UTF_8));
                CoinLot lot = new CoinLot(lotId, operationId, denomination, count, 0, 0);
                lots.put(lotId, lot);
                created.add(lot);
                quantities.put(lotId, count);
                units -= count;
            }
        }
        if (remainder != 0L || created.isEmpty()) {
            frozen = true;
            setDirty();
            return new WithdrawLedgerResult(PhysicalStatus.QUARANTINED, operationId, amount, List.copyOf(created));
        }
        physicalOperations.put(operationId, new PhysicalCoinOperation(operationId, accountId,
                PhysicalCoinOperation.Kind.WITHDRAW, PhysicalCoinOperation.Phase.PREPARED, amount, Map.copyOf(quantities)));
        trimPhysicalHistory();
        setDirty();
        return new WithdrawLedgerResult(PhysicalStatus.PREPARED, operationId, amount, List.copyOf(created));
    }

    public synchronized WithdrawLedgerResult prepareSystemIssuance(SystemAccount source, long amount, UUID operationId) {
        if (source == null || source == SystemAccount.PHYSICAL_OUTSTANDING || source == SystemAccount.MARKET_ESCROW) {
            return new WithdrawLedgerResult(PhysicalStatus.INVALID, operationId, 0L, List.of());
        }
        return prepareWithdraw(source.id(), amount, operationId);
    }

    public synchronized boolean commitWithdraw(UUID operationId) {
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.WITHDRAW) return false;
        if (operation.phase() == PhysicalCoinOperation.Phase.COMMITTED) return true;
        if (operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return false;
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.COMMITTED));
        setDirty();
        return true;
    }

    public synchronized void quarantineWithdraw(UUID operationId) {
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.WITHDRAW ||
                operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return;
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.QUARANTINED));
        frozen = true;
        setDirty();
    }

    public synchronized DepositReservation prepareDeposit(UUID accountId, List<DepositCandidate> candidates, UUID operationId) {
        if (frozen || accountId == null || operationId == null || candidates == null || candidates.isEmpty()) {
            return new DepositReservation(PhysicalStatus.INVALID, operationId, 0L, List.of());
        }
        if (physicalOperations.containsKey(operationId)) return new DepositReservation(PhysicalStatus.REPLAYED, operationId, 0L, List.of());
        Map<UUID, Integer> allocated = new LinkedHashMap<>();
        List<DepositSelection> selections = new ArrayList<>();
        long value = 0L;
        for (DepositCandidate candidate : candidates) {
            if (candidate.count() <= 0) continue;
            CoinLot lot = lots.get(candidate.lotId());
            if (lot == null || lot.denomination() != candidate.denomination()) {
                selections.add(new DepositSelection(candidate.slot(), candidate.lotId(), candidate.denomination(), candidate.count(), 0));
                continue;
            }
            int alreadyAllocated = allocated.getOrDefault(lot.id(), 0);
            int accepted = Math.min(candidate.count(), Math.max(0, lot.available() - alreadyAllocated));
            selections.add(new DepositSelection(candidate.slot(), lot.id(), lot.denomination(), candidate.count(), accepted));
            if (accepted > 0) {
                allocated.put(lot.id(), alreadyAllocated + accepted);
                try { value = Math.addExact(value, Math.multiplyExact((long) accepted, lot.denomination().value())); }
                catch (ArithmeticException overflow) { return new DepositReservation(PhysicalStatus.INVALID, operationId, 0L, List.of()); }
            }
        }
        if (value <= 0L || physicalOutstanding() < value) {
            return new DepositReservation(PhysicalStatus.INVALID, operationId, 0L, List.copyOf(selections));
        }
        allocated.forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.reserve(count)));
        physicalOperations.put(operationId, new PhysicalCoinOperation(operationId, accountId,
                PhysicalCoinOperation.Kind.DEPOSIT, PhysicalCoinOperation.Phase.PREPARED, value, Map.copyOf(allocated)));
        trimPhysicalHistory();
        setDirty();
        return new DepositReservation(PhysicalStatus.PREPARED, operationId, value, List.copyOf(selections));
    }

    public synchronized boolean commitDeposit(UUID operationId) {
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.DEPOSIT ||
                operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return false;
        TransferResult credit = transfer(SystemAccount.PHYSICAL_OUTSTANDING.id(), operation.accountId(), operation.value(), "deposit:" + operationId);
        if (credit.status() != TransferResult.Status.COMMITTED) {
            physicalOperations.put(operation.id(), operation.withPhase(PhysicalCoinOperation.Phase.QUARANTINED));
            frozen = true;
            setDirty();
            return false;
        }
        operation.lotQuantities().forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.commitReserved(count)));
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.COMMITTED));
        trimPhysicalHistory();
        setDirty();
        return true;
    }

    public synchronized void cancelDeposit(UUID operationId) {
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.DEPOSIT ||
                operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return;
        operation.lotQuantities().forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.cancelReserved(count)));
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.QUARANTINED));
        setDirty();
    }

    public synchronized int redeemable(UUID lotId, CoinDenomination denomination) {
        CoinLot lot = lots.get(lotId);
        return lot == null || lot.denomination() != denomination ? 0 : lot.available();
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        tag.putInt("Schema", SCHEMA);
        tag.putBoolean("Frozen", frozen);
        CompoundTag balanceTag = new CompoundTag();
        balances.forEach((id, amount) -> balanceTag.putLong(id.toString(), amount));
        CompoundTag receiptTag = new CompoundTag();
        receipts.forEach((key, id) -> receiptTag.putUUID(key, id));
        ListTag lotTag = new ListTag();
        lots.values().forEach(lot -> lotTag.add(lot.save()));
        ListTag operationTag = new ListTag();
        physicalOperations.values().forEach(operation -> operationTag.add(operation.save()));
        tag.put("Balances", balanceTag);
        tag.put("Receipts", receiptTag);
        tag.put("PhysicalLots", lotTag);
        tag.put("PhysicalOperations", operationTag);
        return tag;
    }

    private static EconomySavedData load(CompoundTag tag) {
        EconomySavedData data = new EconomySavedData();
        CompoundTag balanceTag = tag.getCompound("Balances");
        for (String key : balanceTag.getAllKeys()) {
            try { data.balances.put(UUID.fromString(key), Math.max(0L, balanceTag.getLong(key))); }
            catch (IllegalArgumentException ignored) { }
        }
        for (SystemAccount account : SystemAccount.values()) data.balances.putIfAbsent(account.id(), account.initialBalance());
        CompoundTag receiptTag = tag.getCompound("Receipts");
        for (String key : receiptTag.getAllKeys()) if (receiptTag.hasUUID(key)) data.receipts.put(key, receiptTag.getUUID(key));
        ListTag lotTag = tag.getList("PhysicalLots", Tag.TAG_COMPOUND);
        for (int index = 0; index < lotTag.size(); index++) {
            CoinLot lot = CoinLot.load(lotTag.getCompound(index));
            data.lots.put(lot.id(), lot);
        }
        ListTag operationTag = tag.getList("PhysicalOperations", Tag.TAG_COMPOUND);
        for (int index = 0; index < operationTag.size(); index++) {
            PhysicalCoinOperation operation = PhysicalCoinOperation.load(operationTag.getCompound(index));
            data.physicalOperations.put(operation.id(), operation);
        }
        data.frozen = tag.getBoolean("Frozen") || !data.conservationValid();
        data.trimReceipts();
        data.trimPhysicalHistory();
        return data;
    }

    private boolean conservationValid() {
        try {
            long total = 0L;
            for (long amount : balances.values()) total = Math.addExact(total, amount);
            return total == INITIAL_SUPPLY;
        } catch (ArithmeticException overflow) { return false; }
    }

    private void trimReceipts() {
        while (receipts.size() > MAX_RECEIPTS) receipts.remove(receipts.keySet().iterator().next());
    }

    private void trimPhysicalHistory() {
        while (physicalOperations.size() > MAX_OPERATIONS) {
            UUID removable = physicalOperations.entrySet().stream()
                    .filter(entry -> entry.getValue().phase() == PhysicalCoinOperation.Phase.COMMITTED || entry.getValue().phase() == PhysicalCoinOperation.Phase.QUARANTINED)
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (removable == null) break;
            physicalOperations.remove(removable);
        }
        while (lots.size() > MAX_LOTS) {
            UUID removable = lots.entrySet().stream()
                    .filter(entry -> entry.getValue().redeemed() >= entry.getValue().issued() && entry.getValue().reserved() == 0)
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (removable == null) break;
            lots.remove(removable);
        }
    }

    public enum PhysicalStatus { PREPARED, REPLAYED, INSUFFICIENT_FUNDS, INVALID, QUARANTINED }
    public record WithdrawLedgerResult(PhysicalStatus status, UUID operationId, long value, List<CoinLot> lots) { }
    public record DepositCandidate(int slot, UUID lotId, CoinDenomination denomination, int count) { }
    public record DepositSelection(int slot, UUID lotId, CoinDenomination denomination, int requested, int accepted) { }
    public record DepositReservation(PhysicalStatus status, UUID operationId, long value, List<DepositSelection> selections) { }
}
