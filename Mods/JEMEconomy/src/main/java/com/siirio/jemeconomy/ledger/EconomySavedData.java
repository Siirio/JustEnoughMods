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
    private final Map<CoinDenomination, UUID> coinPools = new java.util.EnumMap<>(CoinDenomination.class);

    /** A world-local bearer series. Operations stay in the journal, never in stack identity. */
    private CoinLot poolIssue(CoinDenomination denomination, int count) {
        UUID id = coinPools.computeIfAbsent(denomination, ignored -> UUID.randomUUID());
        CoinLot prior = lots.get(id);
        int outstanding = prior == null ? 0 : Math.subtractExact(prior.issued(), prior.redeemed());
        lots.put(id, new CoinLot(id, id, denomination, Math.addExact(outstanding, count), 0, prior == null ? 0 : prior.reserved()));
        return new CoinLot(id, id, denomination, count, 0, 0);
    }

    public synchronized net.minecraft.world.item.ItemStack coinTemplate(CoinDenomination denomination, int count) {
        if (frozen()) return net.minecraft.world.item.ItemStack.EMPTY;
        CoinLot pool = poolIssue(denomination, 0);
        setDirty();
        return com.siirio.jemeconomy.physical.CoinProvenance.issue(denomination, count, pool.id(), pool.issuanceOperation());
    }

    private List<CoinLot> operationOutputs(Map<UUID, Integer> quantities) {
        List<CoinLot> result = new ArrayList<>();
        quantities.forEach((id, units) -> {
            CoinLot lot = lots.get(id);
            if (lot == null) return;
            for (int remaining = units; remaining > 0; remaining -= Math.min(64, remaining))
                result.add(new CoinLot(id, lot.issuanceOperation(), lot.denomination(), Math.min(64, remaining), 0, 0));
        });
        return List.copyOf(result);
    }

    /** Consume only proven legacy backing, preserving physical liability and unrelated stack data. */
    public synchronized void normalizeCoin(net.minecraft.world.item.ItemStack stack) {
        if (frozen() || stack.isEmpty()) return;
        var stamp = com.siirio.jemeconomy.physical.CoinProvenance.read(stack);
        if (stamp.isEmpty() || coinPools.containsValue(stamp.get().lotId())) return;
        CoinLot old = lots.get(stamp.get().lotId());
        if (old == null || old.denomination() != stamp.get().denomination()
                || !old.issuanceOperation().equals(stamp.get().operationId()) || old.available() < stack.getCount()) return;
        boolean pending = physicalOperations.values().stream().anyMatch(op -> op.phase() == PhysicalCoinOperation.Phase.PREPARED
                && (op.lotQuantities().containsKey(old.id()) || op.outputLots().containsKey(old.id())));
        if (pending) return;
        CoinLot pooled = poolIssue(old.denomination(), stack.getCount());
        lots.put(old.id(), old.reserve(stack.getCount()).commitReserved(stack.getCount()));
        var replacement = com.siirio.jemeconomy.physical.CoinProvenance.issue(pooled.denomination(), pooled.issued(), pooled.id(), pooled.issuanceOperation());
        stack.getOrCreateTag().put("JemPhysicalCoin", replacement.getTagElement("JemPhysicalCoin").copy());
        setDirty();
    }
    private final LinkedHashMap<UUID, PhysicalCoinOperation> physicalOperations = new LinkedHashMap<>();
    private boolean frozen;
    private String frozenReason = "";
    private UUID developmentAccount;
    private long developmentIssued;
    private MinecraftServer developmentServer;
    private boolean developmentWalletActivated;
    private long testIssued;
    private long settlementIncomeIssued;
    private final Map<UUID, Long> settlementIncomeDays = new LinkedHashMap<>();
    private final Map<UUID, Long> settlementIncomeAmounts = new LinkedHashMap<>();

    /** User-authorized NPC income issuance only; ordinary wallet, market and starter rules remain funded. */
    public synchronized TransferResult issueSettlementIncome(UUID town, UUID treasury, long amount, long day) {
        if (!com.siirio.jemeconomy.api.EconomyApi.isEnabled(developmentServer) || !developmentServer.isSameThread()
                || frozen() || town == null || treasury == null || amount <= 0L || day < 0L)
            return new TransferResult(TransferResult.Status.INVALID, null, 0L, 0L);
        long prior = settlementIncomeDays.getOrDefault(town, -1L);
        if (day <= prior) return new TransferResult(day == prior && settlementIncomeAmounts.getOrDefault(town, -1L) == amount
                ? TransferResult.Status.REPLAYED : TransferResult.Status.INVALID, null, 0L, balance(treasury));
        long issued = Math.addExact(settlementIncomeIssued, amount);
        Math.addExact(INITIAL_SUPPLY, Math.addExact(issued, Math.addExact(testIssued, developmentIssued)));
        long next = Math.addExact(balance(treasury), amount);
        balances.put(treasury, next); settlementIncomeIssued = issued;
        settlementIncomeDays.put(town, day); settlementIncomeAmounts.put(town, amount); setDirty();
        return new TransferResult(TransferResult.Status.COMMITTED, UUID.nameUUIDFromBytes(("settlementIncome:" + town + ":" + day).getBytes(StandardCharsets.UTF_8)), 0L, next);
    }
    private UUID testSaveIdentity;
    private String testEpoch = "";
    private final java.util.Set<UUID> testRecipients = new java.util.HashSet<>();
    private final Map<UUID, com.siirio.jemeconomy.policy.RationalPrice> saleRemainders = new LinkedHashMap<>();
    private final Map<String, SaleOperation> saleOperations = new LinkedHashMap<>();
    private final Map<UUID, String> activeSales = new LinkedHashMap<>();
    private final Map<UUID, String> walletReleases = new LinkedHashMap<>();

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
        if (!com.siirio.jemeconomy.api.EconomyApi.isEnabled(server) || server != developmentServer || !server.isSameThread() || account == null || frozen
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
        if (com.siirio.jemeconomy.api.EconomyApi.isEnabled(developmentServer) && !frozen && developmentAccount != null) {
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
        if (!com.siirio.jemeconomy.api.EconomyApi.isEnabled(developmentServer)) return true;
        if (!frozen && (testIssued != 0L || testSaveIdentity != null) && !testBindingMatches())
            freezeDevelopmentLedger("Test currency is outside its bound save identity/epoch; refusing account writes");
        if (!frozen && developmentAccount != null && !localDevelopmentMarkerMatches(developmentServer, developmentAccount))
            freezeDevelopmentLedger("Development currency requires its dedicated local JVM opt-in and matching account marker; do not deploy this world ledger");
        return frozen;
    }

    public record SaleQuote(long payout, long remainderNumerator, long remainderDenominator, long fundingRequired) {}
    public enum SaleStatus { RESERVED, REPLAYED, INSUFFICIENT_FUNDS, BUSY, INVALID, CANCELLED, COMMITTED }
    public record SaleReservation(SaleStatus status, SaleQuote quote) {}
    private record SaleOperation(UUID account, long numerator, long denominator,
                                 com.siirio.jemeconomy.policy.RationalPrice before, SaleQuote quote, SaleStatus status) {}

    public synchronized SaleQuote quoteSale(UUID account, long numerator, long denominator) {
        var before = saleRemainders.getOrDefault(account, com.siirio.jemeconomy.policy.RationalPrice.ZERO);
        var total = before.add(new com.siirio.jemeconomy.policy.RationalPrice(numerator, denominator));
        var after = total.remainder();
        long funding = Math.subtractExact(Math.addExact(total.floor(), after.ceil()), before.ceil());
        return new SaleQuote(total.floor(), after.numerator(), after.denominator(), funding);
    }

    /** Reserves full payout plus rounded-up fractional liability in the existing escrow account. */
    public synchronized SaleReservation reserveSale(UUID account, String key, long numerator, long denominator) {
        if (account == null || key == null || key.length() > 256 || frozen() || numerator <= 0 || denominator <= 0)
            return new SaleReservation(SaleStatus.INVALID, null);
        var prior = saleOperations.get(key);
        if (prior != null) {
            if (!prior.account.equals(account) || prior.numerator != numerator || prior.denominator != denominator)
                return new SaleReservation(SaleStatus.INVALID, null);
            return new SaleReservation(prior.status == SaleStatus.RESERVED || prior.status == SaleStatus.COMMITTED
                    ? SaleStatus.REPLAYED : prior.status, prior.quote);
        }
        if (activeSales.containsKey(account) || saleOperations.size() >= MAX_OPERATIONS)
            return new SaleReservation(SaleStatus.BUSY, null);
        var quote = quoteSale(account, numerator, denominator);
        if (quote.payout <= 0) return new SaleReservation(SaleStatus.INVALID, quote);
        long market = balance(SystemAccount.NPC_MARKET.id());
        if (market < quote.fundingRequired) return new SaleReservation(SaleStatus.INSUFFICIENT_FUNDS, quote);
        long escrow = Math.addExact(balance(SystemAccount.MARKET_ESCROW.id()), quote.fundingRequired);
        var before = saleRemainders.getOrDefault(account, com.siirio.jemeconomy.policy.RationalPrice.ZERO);
        balances.put(SystemAccount.NPC_MARKET.id(), Math.subtractExact(market, quote.fundingRequired));
        balances.put(SystemAccount.MARKET_ESCROW.id(), escrow);
        saleRemainders.put(account, new com.siirio.jemeconomy.policy.RationalPrice(quote.remainderNumerator, quote.remainderDenominator));
        saleOperations.put(key, new SaleOperation(account, numerator, denominator, before, quote, SaleStatus.RESERVED));
        activeSales.put(account, key); setDirty();
        return new SaleReservation(SaleStatus.RESERVED, quote);
    }

    /** Caller may cancel only before consuming inventory or issuing payment. */
    public synchronized boolean cancelSale(UUID account, String key) {
        var op = saleOperations.get(key);
        if (op == null || !op.account.equals(account) || op.status != SaleStatus.RESERVED || frozen()) return false;
        long escrow = Math.subtractExact(balance(SystemAccount.MARKET_ESCROW.id()), op.quote.fundingRequired);
        if (escrow < 0) return false;
        balances.put(SystemAccount.MARKET_ESCROW.id(), escrow);
        balances.put(SystemAccount.NPC_MARKET.id(), Math.addExact(balance(SystemAccount.NPC_MARKET.id()), op.quote.fundingRequired));
        saleRemainders.put(account, op.before);
        saleOperations.put(key, new SaleOperation(account, op.numerator, op.denominator, op.before, op.quote, SaleStatus.CANCELLED));
        activeSales.remove(account, key); setDirty(); return true;
    }

    /** Finish after durable inventory and payment commit; a retry never advances the fractional claim. */
    public synchronized boolean commitSale(UUID account, String key) {
        var op = saleOperations.get(key);
        if (op == null || !op.account.equals(account) || op.status == SaleStatus.CANCELLED || frozen()) return false;
        if (op.status == SaleStatus.COMMITTED) return true;
        saleOperations.put(key, new SaleOperation(account, op.numerator, op.denominator, op.before, op.quote, SaleStatus.COMMITTED));
        activeSales.remove(account, key); setDirty(); return true;
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

    /** Finite test issuance, never a refill. Identity is resolved at this owner before mutation. */
    public synchronized boolean ensureTestGrant(net.minecraft.server.level.ServerPlayer player) {
        if (player == null || player.getServer() != developmentServer || !developmentServer.isSameThread()) return false;
        var principal = com.siirio.jemidentity.api.IdentityPolicy.resolve(player);
        var binding = com.siirio.jemidentity.api.DedicatedTestProfile.binding(developmentServer);
        if (principal.isEmpty() || binding.isEmpty() || frozen() || developmentAccount != null) return false;
        UUID account = principal.get().accountId();
        if (isSystemAccount(account)) return false;
        var profile = binding.get();
        if (testSaveIdentity != null && (!testSaveIdentity.equals(profile.saveIdentity()) || !testEpoch.equals(profile.epoch())))
            throw new IllegalStateException("Explicit test epoch migration required");
        if (testRecipients.contains(account)) return true;
        long issuance = 100_000_000L;
        long released = balance(account);
        long reserve = Math.addExact(balance(SystemAccount.CENTRAL_RESERVE.id()), released);
        long issued = Math.addExact(testIssued, issuance);
        Math.addExact(INITIAL_SUPPLY, issued);
        testSaveIdentity = profile.saveIdentity(); testEpoch = profile.epoch();
        balances.put(SystemAccount.CENTRAL_RESERVE.id(), reserve);
        balances.put(account, issuance); testIssued = issued; testRecipients.add(account);
        starterRecipients.add(account);
        setDirty();
        if (!conservationValid()) throw new IllegalStateException("Test issuance conservation failure");
        return true;
    }

    private boolean testBindingMatches() {
        var binding = com.siirio.jemidentity.api.DedicatedTestProfile.binding(developmentServer);
        return binding.isPresent() && binding.get().saveIdentity().equals(testSaveIdentity)
                && binding.get().epoch().equals(testEpoch);
    }

    private static boolean isSystemAccount(UUID account) {
        for (SystemAccount system : SystemAccount.values()) if (system.id().equals(account)) return true;
        return false;
    }

    public record WalletRebasePreflight(int accounts, long existing, long target, long released,
                                       long fundingBalance, long shortfall, long physicalLiability) {}

    /** Caller supplies verified personal accounts, never town/escrow identifiers. No writes. */
    public synchronized WalletRebasePreflight preflightWalletRebase(String release, java.util.Set<UUID> accounts,
                                                                    SystemAccount funding) {
        if (release == null || !release.matches("[A-Za-z0-9._-]{1,64}") || accounts == null
                || funding == null || funding == SystemAccount.PHYSICAL_OUTSTANDING || funding == SystemAccount.MARKET_ESCROW)
            throw new IllegalArgumentException("Invalid wallet migration terms");
        long existing = 0L; int count = 0;
        for (UUID account : accounts) {
            if (account == null || isSystemAccount(account) || !starterRecipients.contains(account))
                throw new IllegalArgumentException("Migration account is not an established personal wallet");
            if (release.equals(walletReleases.get(account))) continue;
            existing = Math.addExact(existing, balance(account)); count++;
        }
        long target = Math.multiplyExact((long) count, 2_000L);
        long available = Math.addExact(balance(funding.id()), existing);
        return new WalletRebasePreflight(count, existing, target, Math.max(0L, existing-target),
                balance(funding.id()), Math.max(0L, target-available), physicalOutstanding());
    }

    /** One atomic SavedData transition retains funding, balances and release receipts together. */
    public synchronized WalletRebasePreflight rebaseWallets(String release, java.util.Set<UUID> accounts, SystemAccount funding) {
        if (developmentServer == null || !developmentServer.isDedicatedServer() || !developmentServer.isSameThread()
                || frozen() || developmentAccount != null || testIssued != 0L || testSaveIdentity != null
                || com.siirio.jemidentity.api.DedicatedTestProfile.active(developmentServer))
            throw new IllegalStateException("Production wallet migration unavailable in this profile");
        WalletRebasePreflight plan = preflightWalletRebase(release, accounts, funding);
        if (plan.shortfall() > 0L) return plan;
        long funded = Math.subtractExact(Math.addExact(plan.fundingBalance(), plan.existing()), plan.target());
        for (UUID account : accounts) if (!release.equals(walletReleases.get(account))) {
            balances.put(account, 2_000L); walletReleases.put(account, release);
        }
        balances.put(funding.id(), funded); setDirty();
        if (!conservationValid()) throw new IllegalStateException("Wallet migration conservation failure");
        return plan;
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
        if (frozen() || source == null || first == null || second == null || source.equals(first) || source.equals(second) || first.equals(second)
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
        if(frozen()||first==null||second==null||destination==null||first.equals(second)||destination.equals(first)||destination.equals(second)
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
        return prepareWithdraw(accountId, amount, null, operationId);
    }

    public synchronized WithdrawLedgerResult prepareWithdrawExact(UUID accountId, CoinDenomination denomination,
            int count, UUID operationId) {
        if (denomination == null || count <= 0 || count > 4_096)
            return new WithdrawLedgerResult(PhysicalStatus.INVALID, operationId, 0L, List.of());
        return prepareWithdraw(accountId, denomination.value() * count, denomination, operationId);
    }

    private WithdrawLedgerResult prepareWithdraw(UUID accountId, long amount, CoinDenomination exact, UUID operationId) {
        if (frozen() || accountId == null || operationId == null || amount <= 0L || amount > 1_000_000_000L) {
            return new WithdrawLedgerResult(PhysicalStatus.INVALID, operationId, 0L, List.of());
        }
        PhysicalCoinOperation replay = physicalOperations.get(operationId);
        if (replay != null) {
            List<CoinLot> replayLots = operationOutputs(replay.lotQuantities());
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
        for (CoinDenomination denomination : exact == null ? CoinDenomination.descending() : new CoinDenomination[]{exact}) {
            long units = remainder / denomination.value();
            remainder %= denomination.value();
            while (units > 0L) {
                int count = (int) Math.min(64L, units);
                CoinLot lot = poolIssue(denomination, count);
                UUID lotId = lot.id();
                created.add(lot);
                quantities.merge(lotId, count, Math::addExact);
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
        if (source == null || source == SystemAccount.PHYSICAL_OUTSTANDING) {
            return new WithdrawLedgerResult(PhysicalStatus.INVALID, operationId, 0L, List.of());
        }
        return prepareWithdraw(source.id(), amount, operationId);
    }

    public synchronized boolean commitWithdraw(UUID operationId) {
        if (frozen()) return false;
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.WITHDRAW) return false;
        if (operation.phase() == PhysicalCoinOperation.Phase.COMMITTED) return true;
        if (operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return false;
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.COMMITTED));
        setDirty();
        return true;
    }

    public synchronized void quarantineWithdraw(UUID operationId) {
        if (frozen()) return;
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.WITHDRAW ||
                operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return;
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.QUARANTINED));
        frozen = true;
        setDirty();
    }

    public synchronized DepositReservation prepareDeposit(UUID accountId, List<DepositCandidate> candidates, UUID operationId) {
        if (frozen() || accountId == null || operationId == null || candidates == null || candidates.isEmpty()) {
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
        if (frozen()) return false;
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
        if (frozen()) return;
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.DEPOSIT ||
                operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return;
        operation.lotQuantities().forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.cancelReserved(count)));
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.QUARANTINED));
        setDirty();
    }

    public synchronized ExchangeReservation prepareExchange(UUID accountId, List<DepositCandidate> candidates,
            CoinDenomination source, int sourceCount, CoinDenomination target, UUID operationId) {
        if (frozen() || accountId == null || candidates == null || source == null || target == null || source == target
                || sourceCount <= 0 || sourceCount > 4_096 || operationId == null)
            return new ExchangeReservation(PhysicalStatus.INVALID, operationId, 0L, List.of(), List.of());
        PhysicalCoinOperation replay = physicalOperations.get(operationId);
        if (replay != null) {
            List<CoinLot> replayOutputs = operationOutputs(replay.outputLots());
            return new ExchangeReservation(replay.phase() == PhysicalCoinOperation.Phase.QUARANTINED ? PhysicalStatus.QUARANTINED : PhysicalStatus.REPLAYED,
                    operationId, replay.value(), List.of(), replayOutputs);
        }
        CoinDenomination.Conversion conversion = source.conversionTo(target, sourceCount);
        if (!conversion.possible())
            return new ExchangeReservation(PhysicalStatus.INVALID, operationId, 0L, List.of(), List.of());
        Map<UUID, Integer> observed = new LinkedHashMap<>();
        long available = 0L;
        for (DepositCandidate candidate : candidates) {
            CoinLot lot = lots.get(candidate.lotId());
            if (candidate.count() <= 0 || lot == null || lot.denomination() != source || candidate.denomination() != source) continue;
            int already = observed.getOrDefault(lot.id(), 0);
            int backed = Math.min(candidate.count(), Math.max(0, lot.available() - already));
            observed.put(lot.id(), already + backed);
            available += backed;
            if (available >= sourceCount) break;
        }
        if (available < sourceCount)
            return new ExchangeReservation(PhysicalStatus.INVALID, operationId, 0L, List.of(), List.of());
        long value = conversion.value();
        int remaining = conversion.sourceCount();
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
        long units = conversion.targetCount();
        int sequence = 0;
        while (units > 0L) {
            int count = (int) Math.min(64L, units);
            CoinLot lot = poolIssue(target, count);
            UUID lotId = lot.id();
            outputs.add(lot);
            outputQuantities.merge(lotId, count, Math::addExact);
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
        if (frozen()) return false;
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
        if (frozen()) return;
        PhysicalCoinOperation operation = physicalOperations.get(operationId);
        if (operation == null || operation.kind() != PhysicalCoinOperation.Kind.EXCHANGE || operation.phase() != PhysicalCoinOperation.Phase.PREPARED) return;
        operation.lotQuantities().forEach((lotId, count) -> lots.computeIfPresent(lotId, (ignored, lot) -> lot.cancelReserved(count)));
        operation.outputLots().forEach((id, count) -> lots.computeIfPresent(id, (ignored, lot) -> {
            if (coinPools.containsValue(id)) return new CoinLot(id, id, lot.denomination(), lot.issued() - count, lot.redeemed(), lot.reserved());
            return null;
        }));
        physicalOperations.put(operationId, operation.withPhase(PhysicalCoinOperation.Phase.QUARANTINED));
        setDirty();
    }

    public synchronized int redeemable(UUID lotId, CoinDenomination denomination) {
        CoinLot lot = lots.get(lotId);
        return lot == null || lot.denomination() != denomination ? 0 : lot.available();
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        tag.putLong("SettlementIncomeIssued", settlementIncomeIssued);
        ListTag income = new ListTag(); settlementIncomeDays.forEach((town, day) -> { CompoundTag row = new CompoundTag(); row.putUUID("Town", town); row.putLong("Day", day); row.putLong("Amount", settlementIncomeAmounts.get(town)); income.add(row); }); tag.put("SettlementIncome", income);
        CompoundTag pools = new CompoundTag(); coinPools.forEach((denomination, id) -> pools.putUUID(denomination.id(), id)); tag.put("CoinPools", pools);
        CompoundTag fractions = new CompoundTag();
        saleRemainders.forEach((id, value) -> { CompoundTag row = new CompoundTag(); row.putLong("N", value.numerator()); row.putLong("D", value.denominator()); fractions.put(id.toString(), row); });
        tag.put("SaleRemainders", fractions);
        ListTag sales = new ListTag();
        saleOperations.forEach((key, op) -> { CompoundTag row = new CompoundTag(); row.putString("Key", key); row.putUUID("Account", op.account);
            row.putLong("N", op.numerator); row.putLong("D", op.denominator); row.putLong("BeforeN", op.before.numerator()); row.putLong("BeforeD", op.before.denominator());
            row.putLong("Payout", op.quote.payout); row.putLong("AfterN", op.quote.remainderNumerator); row.putLong("AfterD", op.quote.remainderDenominator);
            row.putLong("Funding", op.quote.fundingRequired); row.putString("Status", op.status.name()); sales.add(row); });
        tag.put("SaleOperations", sales);
        tag.putInt("Schema", SCHEMA);
        tag.putBoolean("Frozen", frozen);
        if (!frozenReason.isEmpty()) tag.putString("FrozenReason", frozenReason);
        if (developmentAccount != null) tag.putUUID("DevelopmentAccount", developmentAccount);
        tag.putLong("DevelopmentIssued", developmentIssued);
        tag.putLong("TestIssued", testIssued);
        if (testSaveIdentity != null) tag.putUUID("TestSaveIdentity", testSaveIdentity);
        tag.putString("TestEpoch", testEpoch);
        ListTag testGrants = new ListTag(); testRecipients.stream().sorted().forEach(id -> testGrants.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));
        tag.put("TestRecipients", testGrants);
        CompoundTag releases = new CompoundTag(); walletReleases.forEach((id, release) -> releases.putString(id.toString(), release)); tag.put("WalletReleases", releases);
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
        data.settlementIncomeIssued = tag.getLong("SettlementIncomeIssued");
        ListTag income = tag.getList("SettlementIncome", Tag.TAG_COMPOUND);
        for (int i = 0; i < income.size(); i++) { CompoundTag row = income.getCompound(i); UUID town = row.getUUID("Town"); data.settlementIncomeDays.put(town, row.getLong("Day")); data.settlementIncomeAmounts.put(town, row.getLong("Amount")); }
        CompoundTag pools = tag.getCompound("CoinPools");
        for (CoinDenomination denomination : CoinDenomination.values())
            if (pools.hasUUID(denomination.id())) data.coinPools.put(denomination, pools.getUUID(denomination.id()));
        CompoundTag fractions = tag.getCompound("SaleRemainders");
        fractions.getAllKeys().forEach(key -> { var row = fractions.getCompound(key); data.saleRemainders.put(UUID.fromString(key), new com.siirio.jemeconomy.policy.RationalPrice(row.getLong("N"), row.getLong("D"))); });
        ListTag sales = tag.getList("SaleOperations", Tag.TAG_COMPOUND);
        for (int i = 0; i < sales.size(); i++) {
            var row = sales.getCompound(i); var status = SaleStatus.valueOf(row.getString("Status")); UUID account = row.getUUID("Account"); String key = row.getString("Key");
            var quote = new SaleQuote(row.getLong("Payout"), row.getLong("AfterN"), row.getLong("AfterD"), row.getLong("Funding"));
            data.saleOperations.put(key, new SaleOperation(account, row.getLong("N"), row.getLong("D"), new com.siirio.jemeconomy.policy.RationalPrice(row.getLong("BeforeN"), row.getLong("BeforeD")), quote, status));
            if (status == SaleStatus.RESERVED && data.activeSales.put(account, key) != null) throw new IllegalStateException("Overlapping saved fractional sale");
        }
        data.developmentAccount = tag.hasUUID("DevelopmentAccount") ? tag.getUUID("DevelopmentAccount") : null;
        data.developmentIssued = tag.getLong("DevelopmentIssued");
        data.testIssued = tag.getLong("TestIssued");
        data.testSaveIdentity = tag.hasUUID("TestSaveIdentity") ? tag.getUUID("TestSaveIdentity") : null;
        data.testEpoch = tag.getString("TestEpoch");
        ListTag testGrants = tag.getList("TestRecipients", Tag.TAG_STRING);
        for (int i = 0; i < testGrants.size(); i++) data.testRecipients.add(UUID.fromString(testGrants.getString(i)));
        CompoundTag releases = tag.getCompound("WalletReleases");
        releases.getAllKeys().forEach(key -> data.walletReleases.put(UUID.fromString(key), releases.getString(key)));
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
            if (developmentIssued < 0L || testIssued < 0L || (testIssued != 0L || testSaveIdentity != null) && !testBindingMatches()) return false;
            if (settlementIncomeIssued < 0L) return false;
            long supply = Math.addExact(INITIAL_SUPPLY, Math.addExact(testIssued, settlementIncomeIssued));
            if (developmentAccount == null) return developmentIssued == 0L && total == supply;
            return localDevelopmentMarkerMatches(developmentServer, developmentAccount)
                    && testIssued == 0L && total == Math.addExact(supply, developmentIssued);
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
                    .filter(entry -> !coinPools.containsValue(entry.getKey()) && entry.getValue().redeemed() >= entry.getValue().issued() && entry.getValue().reserved() == 0)
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
