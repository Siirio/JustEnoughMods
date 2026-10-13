package com.siirio.jemeconomy.physical;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PhysicalCoinService {
    private PhysicalCoinService() {
    }

    public static WithdrawResult withdraw(ServerPlayer player, long amount, UUID requestId) {
        if (!DedicatedSmp.active(player)) return new WithdrawResult(Status.UNAVAILABLE, 0L, Map.of());
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new WithdrawResult(Status.UNAUTHENTICATED, 0L, Map.of());
        Map<CoinDenomination, Long> breakdown = decompose(amount);
        if (breakdown.isEmpty()) return new WithdrawResult(Status.INVALID, 0L, Map.of());
        int requiredSlots = breakdown.values().stream().mapToInt(count -> (int) ((count + 63L) / 64L)).sum();
        long emptySlots = player.getInventory().items.stream().filter(ItemStack::isEmpty).count();
        if (emptySlots < requiredSlots) return new WithdrawResult(Status.NO_SPACE, 0L, breakdown);

        EconomySavedData data = EconomySavedData.get(player.getServer());
        EconomySavedData.WithdrawLedgerResult plan = data.prepareWithdraw(principal.get().accountId(), amount, requestId);
        if (plan.status() == EconomySavedData.PhysicalStatus.INSUFFICIENT_FUNDS) {
            return new WithdrawResult(Status.INSUFFICIENT_FUNDS, 0L, breakdown);
        }
        if (plan.status() != EconomySavedData.PhysicalStatus.PREPARED) {
            return new WithdrawResult(plan.status() == EconomySavedData.PhysicalStatus.REPLAYED ? Status.REPLAYED : Status.INVALID, 0L, breakdown);
        }
        for (CoinLot lot : plan.lots()) {
            ItemStack issued = CoinProvenance.issue(lot.denomination(), lot.issued(), lot.id(), requestId);
            if (!player.getInventory().add(issued) || !issued.isEmpty()) {
                data.quarantineWithdraw(requestId);
                return new WithdrawResult(Status.QUARANTINED, 0L, breakdown);
            }
        }
        if (!data.commitWithdraw(requestId)) {
            data.quarantineWithdraw(requestId);
            return new WithdrawResult(Status.QUARANTINED, 0L, breakdown);
        }
        player.containerMenu.broadcastChanges();
        return new WithdrawResult(Status.COMMITTED, amount, breakdown);
    }

    public static DepositResult depositAll(ServerPlayer player, UUID requestId) {
        if (!DedicatedSmp.active(player)) return new DepositResult(Status.UNAVAILABLE, 0L, 0, 0);
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new DepositResult(Status.UNAUTHENTICATED, 0L, 0, 0);
        List<EconomySavedData.DepositCandidate> candidates = new ArrayList<>();
        int stamped = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            var stamp = CoinProvenance.read(stack);
            if (stamp.isEmpty()) continue;
            stamped += stack.getCount();
            candidates.add(new EconomySavedData.DepositCandidate(slot, stamp.get().lotId(), stamp.get().denomination(), stack.getCount()));
        }
        if (candidates.isEmpty()) return new DepositResult(Status.NO_VALID_COINS, 0L, 0, 0);

        EconomySavedData data = EconomySavedData.get(player.getServer());
        EconomySavedData.DepositReservation reservation = data.prepareDeposit(principal.get().accountId(), candidates, requestId);
        if (reservation.status() != EconomySavedData.PhysicalStatus.PREPARED) {
            return new DepositResult(reservation.status() == EconomySavedData.PhysicalStatus.REPLAYED ? Status.REPLAYED : Status.NO_VALID_COINS,
                    0L, 0, stamped);
        }
        for (EconomySavedData.DepositSelection selection : reservation.selections()) {
            if (selection.accepted() <= 0) continue;
            ItemStack current = player.getInventory().getItem(selection.slot());
            var stamp = CoinProvenance.read(current);
            if (stamp.isEmpty() || !stamp.get().lotId().equals(selection.lotId()) ||
                    stamp.get().denomination() != selection.denomination() || current.getCount() < selection.accepted()) {
                data.cancelDeposit(requestId);
                return new DepositResult(Status.INVENTORY_CHANGED, 0L, 0, stamped);
            }
        }
        int accepted = 0;
        for (EconomySavedData.DepositSelection selection : reservation.selections()) {
            if (selection.accepted() <= 0) continue;
            player.getInventory().getItem(selection.slot()).shrink(selection.accepted());
            accepted += selection.accepted();
        }
        if (!data.commitDeposit(requestId)) return new DepositResult(Status.QUARANTINED, 0L, accepted, stamped - accepted);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new DepositResult(Status.COMMITTED, reservation.value(), accepted, stamped - accepted);
    }

    public static HeldSummary heldSummary(ServerPlayer player) {
        EconomySavedData data = EconomySavedData.get(player.getServer());
        Map<CoinDenomination, Integer> counts = new EnumMap<>(CoinDenomination.class);
        long value = 0L;
        int invalid = 0;
        Map<UUID, Integer> seenByLot = new LinkedHashMap<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            var stamp = CoinProvenance.read(stack);
            if (stamp.isEmpty()) continue;
            int already = seenByLot.getOrDefault(stamp.get().lotId(), 0);
            int backed = Math.min(stack.getCount(), Math.max(0, data.redeemable(stamp.get().lotId(), stamp.get().denomination()) - already));
            seenByLot.put(stamp.get().lotId(), already + backed);
            counts.merge(stamp.get().denomination(), backed, Integer::sum);
            value = Math.addExact(value, Math.multiplyExact((long) backed, stamp.get().denomination().value()));
            invalid += stack.getCount() - backed;
        }
        return new HeldSummary(value, Map.copyOf(counts), invalid);
    }

    public static Map<CoinDenomination, Long> decompose(long amount) {
        if (amount <= 0L || amount > 1_000_000_000L) return Map.of();
        Map<CoinDenomination, Long> result = new LinkedHashMap<>();
        long remainder = amount;
        for (CoinDenomination denomination : CoinDenomination.descending()) {
            long count = remainder / denomination.value();
            remainder %= denomination.value();
            if (count > 0L) result.put(denomination, count);
        }
        return remainder == 0L ? Map.copyOf(result) : Map.of();
    }

    public enum Status {
        COMMITTED, REPLAYED, INSUFFICIENT_FUNDS, NO_SPACE, NO_VALID_COINS, INVENTORY_CHANGED,
        UNAUTHENTICATED, UNAVAILABLE, INVALID, QUARANTINED
    }

    public record WithdrawResult(Status status, long value, Map<CoinDenomination, Long> breakdown) { }
    public record DepositResult(Status status, long value, int acceptedItems, int rejectedItems) { }
    public record HeldSummary(long value, Map<CoinDenomination, Integer> counts, int invalidItems) { }
}
