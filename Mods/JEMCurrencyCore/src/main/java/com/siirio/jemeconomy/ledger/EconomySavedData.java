package com.siirio.jemeconomy.ledger;

import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemeconomy.physical.CoinLot;
import com.siirio.jemeconomy.physical.PhysicalCoinOperation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    private static final long DEVELOPMENT_WALLET_BALANCE = 1_000_000_000_000L;

    private final Map<UUID, Long> balances = new LinkedHashMap<>();
    private final LinkedHashMap<String, UUID> receipts = new LinkedHashMap<>();
    private final Map<String, String> receiptTerms = new LinkedHashMap<>();
    private final java.util.Set<UUID> starterRecipients = new java.util.HashSet<>();
    private final LinkedHashMap<UUID, CoinLot> lots = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, PhysicalCoinOperation> physicalOperations = new LinkedHashMap<>();
    private boolean frozen;
    private String frozenReason = "";
    private UUID developmentAccount;
    private long developmentIssued;
    private MinecraftServer developmentServer;
    private boolean developmentWalletActivated;

    public EconomySavedData() {
        for (SystemAccount account : SystemAccount.values()) balances.put(account.id(), account.initialBalance());
    }

    public static EconomySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(tag -> load(tag, server), () -> {
            EconomySavedData data = new EconomySavedData();
            data.developmentServer = server;
            return data;
        }, NAME);
    }

    /** Explicit authenticated local activation; replay never resets issuance or clears a frozen ledger. */
    public synchronized boolean enableLocalDevelopmentWallet(MinecraftServer server, UUID account) {
        if (server == null || server != developmentServer || !server.isSameThread() || account == null || frozen
                || !localDevelopmentMarkerMatches(server, account)) return false;
        for (SystemAccount system : SystemAccount.values()) if (system.id().equals(account)) return false;
        if (developmentAccount != null && !developmentAccount.equals(account)) return false;
        developmentAccount = account;
        developmentWalletActivated = true;
        setDirty();
        balance(account);
        return !frozen;
    }

    public synchronized long balance(UUID id) {
        if (!frozen && developmentAccount != null) {
            if (!localDevelopmentMarkerMatches(developmentServer, developmentAccount)) {
                freezeDevelopmentLedger("Development currency requires its dedicated local JVM opt-in and matching account marker; do not deploy this world ledger");
            } else if (developmentWalletActivated && developmentAccount.equals(id)) {
                long current = balances.getOrDefault(id, 0L);
                if (current < DEVELOPMENT_WALLET_BALANCE) {
                    try {
                        long issuance = Math.subtractExact(DEVELOPMENT_WALLET_BALANCE, current);
                        long nextIssued = Math.addExact(developmentIssued, issuance);
                        Math.addExact(INITIAL_SUPPLY, nextIssued);
                        developmentIssued = nextIssued;
                        balances.put(id, DEVELOPMENT_WALLET_BALANCE);
                        setDirty();
                    } catch (ArithmeticException overflow) {
                        freezeDevelopmentLedger("Local development currency issuance overflow");
                    }
                }
            }
        }
        return id == null ? 0L : balances.getOrDefault(id, 0L);
    }

    private static boolean localDevelopmentMarkerMatches(MinecraftServer server, UUID account) {
        if (server == null || !server.isDedicatedServer() || !Boolean.getBoolean("jem.localDevelopmentMoney")) return false;
        try {
            var marker = server.getServerDirectory().getCanonicalFile().toPath().resolve("local/unlimited-wallet-account.txt");
            return Files.isRegularFile(marker) && Files.size(marker) <= 128L
                    && account.toString().equals(Files.readString(marker, StandardCharsets.UTF_8).trim());
        } catch (java.io.IOException | SecurityException unavailable) { return false; }
    }

    private void freezeDevelopmentLedger(String reason) {
        frozen = true;
        frozenReason = reason;
        com.mojang.logging.LogUtils.getLogger().error("JEM economy frozen: {}", reason);
        setDirty();
    }

    public synchronized boolean frozen() {
        if (!frozen && developmentAccount != null && !localDevelopmentMarkerMatches(developmentServer, developmentAccount))
            freezeDevelopmentLedger("Development currency requires its dedicated local JVM opt-in and matching account marker; do not deploy this world ledger");
        return frozen;
    }

    public synchronized boolean hasReceipt(String idempotencyKey) { return receipts.containsKey(idempotencyKey); }

    /** Newest retained committed account transfers. Legacy receipts without terms stay unprojected. */
    public synchronized List<AccountReceipt> receiptHistory(UUID account, int requestedLimit) {
        if (account == null || requestedLimit <= 0) return List.of();
        int limit = Math.min(64, requestedLimit);
        List<AccountReceipt> result = new ArrayList<>();
        var entries = new ArrayList<>(receipts.entrySet());
        for (int index = entries.size() - 1; index >= 0 && result.size() < limit; index--) {
            var entry = entries.get(index);
            String terms = receiptTerms.get(entry.getKey());
            if (terms == null) continue;
            String[] parts = terms.split(":", -1);
            if (parts.length != 3) throw new IllegalStateException("Invalid saved transfer terms");
            UUID source = UUID.fromString(parts[0]);
            UUID destination = UUID.fromString(parts[1]);
            long amount = Long.parseLong(parts[2]);
            if (source.equals(account) || destination.equals(account))
                result.add(new AccountReceipt(source, destination, amount, entry.getValue()));
        }
        return List.copyOf(result);
    }

    public record AccountReceipt(UUID source, UUID destination, long amount, UUID receipt) {}

    /** Account-level lifetime fact, independent of bounded transaction history. */
    public synchronized void ensureStarter(UUID accountId, long amount) {
        if (accountId == null || starterRecipients.contains(accountId)) return;
        var transfer = transfer(SystemAccount.CENTRAL_RESERVE.id(), accountId, amount, "starterGrant:" + accountId);
        if (transfer.status() == TransferResult.Status.COMMITTED || transfer.status() == TransferResult.Status.REPLAYED) {
            starterRecipients.add(accountId); setDirty();
        }
    }

    public synchronized long physicalOutstanding() {
        return balance(SystemAccount.PHYSICAL_OUTSTANDING.id());
    }

    public synchronized TransferResult transfer(UUID source, UUID destination, long amount, String idempotencyKey) {
        if (frozen()) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, balance(source), balance(destination));
        if (source == null || destination == null || source.equals(destination) || amount <= 0L ||
                idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 160) {
            return new TransferResult(TransferResult.Status.INVALID, null, balance(source), balance(destination));
        }
        UUID prior = receipts.get(idempotencyKey);
        String terms = source + ":" + destination + ":" + amount;
        if (prior != null) {
            String original = receiptTerms.get(idempotencyKey);
            return new TransferResult(original == null || original.equals(terms) ? TransferResult.Status.REPLAYED : TransferResult.Status.INVALID,
                    prior, balance(source), balance(destination));
        }
        long sourceBalance = balance(source);
        if (sourceBalance < amount) return new TransferResult(TransferResult.Status.INSUFFICIENT_FUNDS, null, sourceBalance, balance(destination));
        long destinationBalance;
        try { destinationBalance = Math.addExact(balance(destination), amount); }
        catch (ArithmeticException overflow) { return new TransferResult(TransferResult.Status.INVALID, null, sourceBalance, balance(destination)); }
        if (frozen()) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, sourceBalance, balance(destination));
        balances.put(source, sourceBalance - amount);
        balances.put(destination, destinationBalance);
        UUID receipt = UUID.nameUUIDFromBytes(("jem:receipt:" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
        receipts.put(idempotencyKey, receipt);
        receiptTerms.put(idempotencyKey, terms);
        trimReceipts();
        if (!conservationValid()) frozen = true;
        setDirty();
        return frozen
                ? new TransferResult(TransferResult.Status.UNAVAILABLE, receipt, sourceBalance - amount, destinationBalance)
                : new TransferResult(TransferResult.Status.COMMITTED, receipt, sourceBalance - amount, destinationBalance);
    }

    /** Both legs share one saved-data mutation. No listener or disk save sees half a tax split. */
    public synchronized TransferResult splitTransfer(UUID source, UUID first, long firstAmount, UUID second, long secondAmount, String key) {
        if (secondAmount == 0) return transfer(source, first, firstAmount, key + ":market");
        if (frozen || source == null || first == null || second == null || source.equals(first) || source.equals(second) || first.equals(second)
                || firstAmount <= 0 || secondAmount <= 0 || key == null || key.length() > 145)
            return new TransferResult(TransferResult.Status.INVALID, null, balance(source), balance(first));
        if (hasReceipt(key + ":market") || hasReceipt(key + ":tax")) {
            if (!hasReceipt(key + ":market") || !hasReceipt(key + ":tax")) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, balance(source), balance(first));
            String marketTerms = source + ":" + first + ":" + firstAmount;
            String taxTerms = source + ":" + second + ":" + secondAmount;
            return new TransferResult(marketTerms.equals(receiptTerms.get(key + ":market")) && taxTerms.equals(receiptTerms.get(key + ":tax"))
                    ? TransferResult.Status.REPLAYED : TransferResult.Status.INVALID, receipts.get(key + ":market"), balance(source), balance(first));
        }
        long total;
        try { total = Math.addExact(firstAmount, secondAmount); Math.addExact(balance(first), firstAmount); Math.addExact(balance(second), secondAmount); }
        catch (ArithmeticException overflow) { return new TransferResult(TransferResult.Status.INVALID, null, balance(source), balance(first)); }
        if (balance(source) < total) return new TransferResult(TransferResult.Status.INSUFFICIENT_FUNDS, null, balance(source), balance(first));
        TransferResult market = transfer(source, first, firstAmount, key + ":market");
        TransferResult tax = transfer(source, second, secondAmount, key + ":tax");
        return tax.status() == TransferResult.Status.COMMITTED ? market : tax;
    }

    /** Reverse a captured two-leg payment without exposing a half-refund to persistence. */
    public synchronized TransferResult mergeTransfer(UUID first,long firstAmount,UUID second,long secondAmount,UUID destination,String key) {
        if(secondAmount==0)return transfer(first,destination,firstAmount,key+":market");
        if(frozen||first==null||second==null||destination==null||first.equals(second)||destination.equals(first)||destination.equals(second)
                ||firstAmount<=0||secondAmount<=0||key==null||key.length()>145)return new TransferResult(TransferResult.Status.INVALID,null,balance(first),balance(destination));
        boolean marketDone=hasReceipt(key+":market"),taxDone=hasReceipt(key+":tax");
        if(marketDone||taxDone){
            if(!marketDone||!taxDone)return new TransferResult(TransferResult.Status.UNAVAILABLE,null,balance(first),balance(destination));
            String firstTerms=first+":"+destination+":"+firstAmount,secondTerms=second+":"+destination+":"+secondAmount;
            return new TransferResult(firstTerms.equals(receiptTerms.get(key+":market"))&&secondTerms.equals(receiptTerms.get(key+":tax"))?TransferResult.Status.REPLAYED:TransferResult.Status.INVALID,receipts.get(key+":market"),balance(first),balance(destination));
        }
        try{Math.addExact(balance(destination),Math.addExact(firstAmount,secondAmount));}catch(ArithmeticException overflow){return new TransferResult(TransferResult.Status.INVALID,null,balance(first),balance(destination));}
        if(balance(first)<firstAmount||balance(second)<secondAmount)return new TransferResult(TransferResult.Status.INSUFFICIENT_FUNDS,null,balance(first),balance(destination));
        TransferResult market=transfer(first,destination,firstAmount,key+":market");TransferResult tax=transfer(second,destination,secondAmount,key+":tax");
        return tax.status()==TransferResult.Status.COMMITTED?market:tax;
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
                PhysicalCoinOperation.Kind.WITHDRAW, PhysicalCoinOperation.Phase.PREPARED, amount, Map.copyOf(quantities), Map.of()));
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
                PhysicalCoinOperation.Kind.DEPOSIT, PhysicalCoinOperation.Phase.PREPARED, value, Map.copyOf(allocated), Map.of()));
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

    public synchronized ExchangeReservation prepareExchange(UUID accountId, List<DepositCandidate> candidates,
            CoinDenomination source, int sourceCount, CoinDenomination target, UUID operationId) {
        if (frozen || accountId == null || candidates == null || source == null || target == null || source == target
                || sourceCount <= 0 || sourceCount > 4_096 || operationId == null)
            return new ExchangeReservation(PhysicalStatus.INVALID, operationId, 0L, List.of(), List.of());
        PhysicalCoinOperation replay = physicalOperations.get(operationId);
        if (replay != null) {
            List<CoinLot> replayOutputs = replay.outputLots().keySet().stream().map(lots::get).filter(Objects::nonNull).toList();
            return new ExchangeReservation(replay.phase() == PhysicalCoinOperation.Phase.QUARANTINED ? PhysicalStatus.QUARANTINED : PhysicalStatus.REPLAYED,
                    operationId, replay.value(), List.of(), replayOutputs);
        }
        long value;
        try { value = Math.multiplyExact((long) sourceCount, source.value()); }
        catch (ArithmeticException overflow) { return new ExchangeReservation(PhysicalStatus.INVALID, operationId, 0L, List.of(), List.of()); }
        if (value % target.value() != 0L || value / target.value() <= 0L || value / target.value() > 4_096L)
            return new ExchangeReservation(PhysicalStatus.INVALID, operationId, 0L, List.of(), List.of());
        int remaining = sourceCount;
        Map<UUID, Integer> allocated = new LinkedHashMap<>();
        List<DepositSelection> selections = new ArrayList<>();
        for (DepositCandidate candidate : candidates) {
            if (remaining <= 0) break;
            CoinLot lot = lots.get(candidate.lotId());
            if (lot == null || lot.denomination() != source || candidate.denomination() != source) continue;
            int alreadyAllocated = allocated.getOrDefault(lot.id(), 0);
            int accepted = Math.min(Math.min(candidate.count(), Math.max(0, lot.available() - alreadyAllocated)), remaining);
            if (accepted <= 0) continue;
            selections.add(new DepositSelection(candidate.slot(), lot.id(), source, candidate.count(), accepted));
            allocated.put(lot.id(), alreadyAllocated + accepted);
            remaining -= accepted;
        }
        if (remaining != 0) return new ExchangeReservation(PhysicalStatus.INVALID, operationId, 0L, List.copyOf(selections), List.of());
        allocated.forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.reserve(count)));
        Map<UUID, Integer> outputQuantities = new LinkedHashMap<>();
        List<CoinLot> outputs = new ArrayList<>();
        long units = value / target.value();
        int sequence = 0;
        while (units > 0L) {
            int count = (int) Math.min(64L, units);
            UUID lotId = UUID.nameUUIDFromBytes((operationId + ":exchange:" + target.id() + ":" + sequence++).getBytes(StandardCharsets.UTF_8));
            CoinLot lot = new CoinLot(lotId, operationId, target, count, 0, 0);
            lots.put(lotId, lot);
            outputs.add(lot);
            outputQuantities.put(lotId, count);
            units -= count;
        }
        physicalOperations.put(operationId, new PhysicalCoinOperation(operationId, accountId,
                PhysicalCoinOperation.Kind.EXCHANGE, PhysicalCoinOperation.Phase.PREPARED, value,
                Map.copyOf(allocated), Map.copyOf(outputQuantities)));
        trimPhysicalHistory();
        setDirty();
        return new ExchangeReservation(PhysicalStatus.PREPARED, operationId, value, List.copyOf(selections), List.copyOf(outputs));
    }

    public synchronized boolean commitExchange(UUID operationId) {
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.EXCHANGE) return false;
        if (operation.phase() == PhysicalCoinOperation.Phase.COMMITTED) return true;
        if (operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return false;
        operation.lotQuantities().forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.commitReserved(count)));
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.COMMITTED));
        setDirty();
        return true;
    }

    public synchronized void cancelExchange(UUID operationId) {
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.EXCHANGE || operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return;
        operation.lotQuantities().forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.cancelReserved(count)));
        operation.outputLots().keySet().forEach(lots::remove);
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
        if (!frozenReason.isEmpty()) tag.putString("FrozenReason", frozenReason);
        if (developmentAccount != null) tag.putUUID("DevelopmentAccount", developmentAccount);
        tag.putLong("DevelopmentIssued", developmentIssued);
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
        CompoundTag termsTag = new CompoundTag(); receiptTerms.forEach(termsTag::putString); tag.put("ReceiptTerms", termsTag);
        ListTag starters = new ListTag(); starterRecipients.stream().sorted().forEach(id -> starters.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));
        tag.put("StarterRecipients", starters);
        tag.put("PhysicalLots", lotTag);
        tag.put("PhysicalOperations", operationTag);
        return tag;
    }

    private static EconomySavedData load(CompoundTag tag, MinecraftServer server) {
        EconomySavedData data = new EconomySavedData();
        data.developmentServer = server;
        data.developmentAccount = tag.hasUUID("DevelopmentAccount") ? tag.getUUID("DevelopmentAccount") : null;
        data.developmentIssued = tag.getLong("DevelopmentIssued");
        data.frozenReason = tag.getString("FrozenReason");
        CompoundTag balanceTag = tag.getCompound("Balances");
        for (String key : balanceTag.getAllKeys()) {
            try { data.balances.put(UUID.fromString(key), Math.max(0L, balanceTag.getLong(key))); }
            catch (IllegalArgumentException ignored) { }
        }
        for (SystemAccount account : SystemAccount.values()) data.balances.putIfAbsent(account.id(), account.initialBalance());
        CompoundTag receiptTag = tag.getCompound("Receipts");
        for (String key : receiptTag.getAllKeys()) if (receiptTag.hasUUID(key)) data.receipts.put(key, receiptTag.getUUID(key));
        CompoundTag termsTag = tag.getCompound("ReceiptTerms");
        for (String key : termsTag.getAllKeys()) if (data.receipts.containsKey(key)) data.receiptTerms.put(key, termsTag.getString(key));
        ListTag starters = tag.getList("StarterRecipients", Tag.TAG_STRING);
        for (int i = 0; i < starters.size(); i++) {
            try { data.starterRecipients.add(UUID.fromString(starters.getString(i))); } catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Invalid starter account", invalid); }
        }
        data.receipts.keySet().stream().filter(key -> key.startsWith("starterGrant:")).forEach(key -> {
            try { data.starterRecipients.add(UUID.fromString(key.substring("starterGrant:".length()))); } catch (IllegalArgumentException ignored) { }
        });
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
        if ((data.developmentAccount != null || data.developmentIssued != 0L)
                && (data.developmentIssued < 0L || data.developmentAccount == null
                    || !localDevelopmentMarkerMatches(server, data.developmentAccount))) {
            data.freezeDevelopmentLedger("Saved development currency is outside its dedicated local opt-in/account marker boundary; do not deploy this world ledger");
        }
        data.trimReceipts();
        data.trimPhysicalHistory();
        return data;
    }

    private boolean conservationValid() {
        try {
            long total = 0L;
            for (long amount : balances.values()) total = Math.addExact(total, amount);
            if (developmentIssued < 0L) return false;
            if (developmentAccount == null) return developmentIssued == 0L && total == INITIAL_SUPPLY;
            return localDevelopmentMarkerMatches(developmentServer, developmentAccount)
                    && total == Math.addExact(INITIAL_SUPPLY, developmentIssued);
        } catch (ArithmeticException overflow) { return false; }
    }

    private void trimReceipts() {
        while (receipts.size() > MAX_RECEIPTS) {
            String key = receipts.keySet().iterator().next(); receipts.remove(key); receiptTerms.remove(key);
        }
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
    public record ExchangeReservation(PhysicalStatus status, UUID operationId, long value,
                                      List<DepositSelection> selections, List<CoinLot> outputs) { }
}
