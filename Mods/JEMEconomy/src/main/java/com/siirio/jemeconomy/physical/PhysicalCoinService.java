package com.siirio.jemeconomy.physical;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemcurrency.CurrencyAuthority;
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
        return withdraw(player, amount, requestId, decompose(amount), null, 0);
    }

    public static WithdrawResult withdrawExact(ServerPlayer player, CoinDenomination denomination, int count, UUID requestId) {
        if (denomination == null || count <= 0 || count > 4_096)
            return new WithdrawResult(Status.INVALID, 0L, Map.of());
        return withdraw(player, denomination.value() * count, requestId,
                Map.of(denomination, (long) count), denomination, count);
    }

    private static WithdrawResult withdraw(ServerPlayer player, long amount, UUID requestId,
            Map<CoinDenomination, Long> breakdown, CoinDenomination exact, int count) {
        if (!CurrencyAuthority.active(player)) return new WithdrawResult(Status.UNAVAILABLE, 0L, Map.of());
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new WithdrawResult(Status.UNAUTHENTICATED, 0L, Map.of());
        if (breakdown.isEmpty()) return new WithdrawResult(Status.INVALID, 0L, Map.of());
        EconomySavedData data = EconomySavedData.get(player.getServer());
        player.getInventory().items.forEach(data::normalizeCoin);
        List<ItemStack> simulated = player.getInventory().items.stream().map(ItemStack::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (var entry : breakdown.entrySet()) {
            long remaining = entry.getValue();
            while (remaining > 0) {
                int stackCount = (int) Math.min(64L, remaining);
                if (!insertSimulated(simulated, data.coinTemplate(entry.getKey(), stackCount)))
                    return new WithdrawResult(Status.NO_SPACE, 0L, breakdown);
                remaining -= stackCount;
            }
        }

        EconomySavedData.WithdrawLedgerResult plan = exact == null
                ? data.prepareWithdraw(principal.get().accountId(), amount, requestId)
                : data.prepareWithdrawExact(principal.get().accountId(), exact, count, requestId);
        if (plan.status() == EconomySavedData.PhysicalStatus.INSUFFICIENT_FUNDS) {
            return new WithdrawResult(Status.INSUFFICIENT_FUNDS, 0L, breakdown);
        }
        if (plan.status() != EconomySavedData.PhysicalStatus.PREPARED) {
            return new WithdrawResult(plan.status() == EconomySavedData.PhysicalStatus.REPLAYED ? Status.REPLAYED : Status.INVALID, 0L, breakdown);
        }
        for (CoinLot lot : plan.lots()) {
            ItemStack issued = CoinProvenance.issue(lot.denomination(), lot.issued(), lot.id(), lot.issuanceOperation());
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
        if (!CurrencyAuthority.active(player)) return new DepositResult(Status.UNAVAILABLE, 0L, 0, 0);
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

    public static DepositResult depositExact(ServerPlayer player, CoinDenomination denomination, int count, UUID requestId) {
        if (!CurrencyAuthority.active(player)) return new DepositResult(Status.UNAVAILABLE, 0L, 0, 0);
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new DepositResult(Status.UNAUTHENTICATED, 0L, 0, 0);
        if (denomination == null || count <= 0 || count > 4_096 || requestId == null)
            return new DepositResult(Status.INVALID, 0L, 0, 0);
        EconomySavedData data = EconomySavedData.get(player.getServer());
        List<EconomySavedData.DepositCandidate> candidates = new ArrayList<>();
        Map<UUID, Integer> selectedByLot = new LinkedHashMap<>();
        int remaining = count;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            var stamp = CoinProvenance.read(stack);
            if (stamp.isEmpty() || stamp.get().denomination() != denomination) continue;
            int already = selectedByLot.getOrDefault(stamp.get().lotId(), 0);
            int backed = Math.min(stack.getCount(), Math.max(0, data.redeemable(stamp.get().lotId(), denomination) - already));
            int take = Math.min(backed, remaining);
            if (take <= 0) continue;
            candidates.add(new EconomySavedData.DepositCandidate(slot, stamp.get().lotId(), denomination, take));
            selectedByLot.put(stamp.get().lotId(), already + take);
            remaining -= take;
        }
        if (remaining != 0) return new DepositResult(Status.NO_VALID_COINS, 0L, 0, count);
        EconomySavedData.DepositReservation reservation = data.prepareDeposit(principal.get().accountId(), candidates, requestId);
        if (reservation.status() != EconomySavedData.PhysicalStatus.PREPARED)
            return new DepositResult(reservation.status() == EconomySavedData.PhysicalStatus.REPLAYED ? Status.REPLAYED : Status.NO_VALID_COINS,
                    0L, 0, count);
        int accepted = reservation.selections().stream().mapToInt(EconomySavedData.DepositSelection::accepted).sum();
        if (accepted != count || reservation.value() != denomination.value() * count) {
            data.cancelDeposit(requestId);
            return new DepositResult(Status.NO_VALID_COINS, 0L, 0, count);
        }
        for (EconomySavedData.DepositSelection selection : reservation.selections()) {
            ItemStack current = player.getInventory().getItem(selection.slot());
            var stamp = CoinProvenance.read(current);
            if (stamp.isEmpty() || !stamp.get().lotId().equals(selection.lotId())
                    || stamp.get().denomination() != denomination || current.getCount() < selection.accepted()) {
                data.cancelDeposit(requestId);
                return new DepositResult(Status.INVENTORY_CHANGED, 0L, 0, count);
            }
        }
        reservation.selections().forEach(selection -> player.getInventory().getItem(selection.slot()).shrink(selection.accepted()));
        if (!data.commitDeposit(requestId)) return new DepositResult(Status.QUARANTINED, 0L, count, 0);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new DepositResult(Status.COMMITTED, reservation.value(), count, 0);
    }

    /** Redeems exactly one backed Gold Coin and moves its value to the central reserve. */
    public static Status spendOneGold(ServerPlayer player, UUID requestId) {
        if (!CurrencyAuthority.active(player) || requestId == null) return Status.UNAVAILABLE;
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Status.UNAUTHENTICATED;
        EconomySavedData data = EconomySavedData.get(player.getServer());
        String paymentKey = "settlementGoldSpend:" + requestId;
        if (data.hasReceipt(paymentKey)) return Status.REPLAYED;
        int slot = -1;
        CoinProvenance.Stamp selected = null;
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack stack = player.getInventory().getItem(index);
            var stamp = CoinProvenance.read(stack);
            if (stamp.isPresent() && stamp.get().denomination() == CoinDenomination.GOLD
                    && data.redeemable(stamp.get().lotId(), CoinDenomination.GOLD) > 0) {
                slot = index;
                selected = stamp.get();
                break;
            }
        }
        UUID depositId = UUID.nameUUIDFromBytes(("settlementGoldDeposit:" + requestId)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (selected == null) return Status.NO_VALID_COINS;
        {
            var reservation = data.prepareDeposit(principal.get().accountId(),
                    List.of(new EconomySavedData.DepositCandidate(slot, selected.lotId(), CoinDenomination.GOLD, 1)), depositId);
            if (reservation.status() == EconomySavedData.PhysicalStatus.PREPARED) {
                ItemStack current = player.getInventory().getItem(slot);
                var currentStamp = CoinProvenance.read(current);
                if (currentStamp.isEmpty() || !currentStamp.get().lotId().equals(selected.lotId())
                        || currentStamp.get().denomination() != CoinDenomination.GOLD || current.isEmpty()) {
                    data.cancelDeposit(depositId);
                    return Status.INVENTORY_CHANGED;
                }
                current.shrink(1);
                if (!data.commitDeposit(depositId)) return Status.QUARANTINED;
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
            } else if (reservation.status() != EconomySavedData.PhysicalStatus.REPLAYED) {
                return Status.NO_VALID_COINS;
            }
        }
        var payment = data.transfer(principal.get().accountId(), SystemAccount.CENTRAL_RESERVE.id(), CoinDenomination.GOLD.value(), paymentKey);
        return switch (payment.status()) {
            case COMMITTED -> Status.COMMITTED;
            case REPLAYED -> Status.REPLAYED;
            case INSUFFICIENT_FUNDS -> Status.INSUFFICIENT_FUNDS;
            default -> Status.QUARANTINED;
        };
    }

    /** Redeems an exact value of backed physical Coins, then moves that value to the reserve. */
    public static Status spend(ServerPlayer player, long amount, UUID requestId, String purpose) {
        if (!CurrencyAuthority.active(player) || requestId == null || amount <= 0L || amount > 1_000_000_000L) return Status.UNAVAILABLE;
        var principal = IdentityPolicy.resolve(player); if (principal.isEmpty()) return Status.UNAUTHENTICATED;
        EconomySavedData data = EconomySavedData.get(player.getServer());
        String paymentKey = purpose + ":" + requestId; if (data.hasReceipt(paymentKey)) return Status.REPLAYED;
        long remaining = amount; List<EconomySavedData.DepositCandidate> candidates = new ArrayList<>(); Map<UUID, Integer> selectedByLot = new LinkedHashMap<>();
        for (CoinDenomination denomination : CoinDenomination.descending()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining >= denomination.value(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot); var stamp = CoinProvenance.read(stack);
                if (stamp.isEmpty() || stamp.get().denomination() != denomination) continue;
                int already = selectedByLot.getOrDefault(stamp.get().lotId(), 0);
                int backed = Math.min(stack.getCount(), Math.max(0, data.redeemable(stamp.get().lotId(), denomination) - already));
                int take = (int) Math.min(backed, remaining / denomination.value());
                if (take <= 0) continue;
                candidates.add(new EconomySavedData.DepositCandidate(slot, stamp.get().lotId(), denomination, take)); selectedByLot.put(stamp.get().lotId(), already + take);
                remaining -= (long) take * denomination.value();
            }
        }
        if (remaining != 0L) return Status.INSUFFICIENT_FUNDS;
        UUID depositId = UUID.nameUUIDFromBytes((purpose + ":deposit:" + requestId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var reservation = data.prepareDeposit(principal.get().accountId(), candidates, depositId);
        if (reservation.status() == EconomySavedData.PhysicalStatus.PREPARED) {
            for (var selection : reservation.selections()) {
                ItemStack current = player.getInventory().getItem(selection.slot()); var stamp = CoinProvenance.read(current);
                if (stamp.isEmpty() || !stamp.get().lotId().equals(selection.lotId()) || stamp.get().denomination() != selection.denomination() || current.getCount() < selection.accepted()) { data.cancelDeposit(depositId); return Status.INVENTORY_CHANGED; }
            }
            reservation.selections().forEach(selection -> player.getInventory().getItem(selection.slot()).shrink(selection.accepted()));
            if (!data.commitDeposit(depositId)) return Status.QUARANTINED;
            player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        } else if (reservation.status() != EconomySavedData.PhysicalStatus.REPLAYED) return Status.NO_VALID_COINS;
        var payment = data.transfer(principal.get().accountId(), SystemAccount.CENTRAL_RESERVE.id(), amount, paymentKey);
        return switch (payment.status()) { case COMMITTED -> Status.COMMITTED; case REPLAYED -> Status.REPLAYED; case INSUFFICIENT_FUNDS -> Status.INSUFFICIENT_FUNDS; default -> Status.QUARANTINED; };
    }

    public static ExchangeResult exchange(ServerPlayer player, CoinDenomination source, int sourceCount,
                                          CoinDenomination target, UUID requestId) {
        if (!CurrencyAuthority.active(player)) return new ExchangeResult(Status.UNAVAILABLE, 0L, 0);
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new ExchangeResult(Status.UNAUTHENTICATED, 0L, 0);
        if (source == null || target == null || source == target || sourceCount <= 0 || sourceCount > 4_096)
            return new ExchangeResult(Status.INVALID, 0L, 0);
        List<EconomySavedData.DepositCandidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            var stamp = CoinProvenance.read(stack);
            if (stamp.isPresent() && stamp.get().denomination() == source)
                candidates.add(new EconomySavedData.DepositCandidate(slot, stamp.get().lotId(), source, stack.getCount()));
        }
        EconomySavedData data = EconomySavedData.get(player.getServer());
        var reservation = data.prepareExchange(principal.get().accountId(), candidates, source, sourceCount, target, requestId);
        if (reservation.status() == EconomySavedData.PhysicalStatus.REPLAYED)
            return new ExchangeResult(Status.REPLAYED, reservation.value(), (int) (reservation.value() / target.value()));
        if (reservation.status() != EconomySavedData.PhysicalStatus.PREPARED)
            return new ExchangeResult(Status.NO_VALID_COINS, 0L, 0);

        List<ItemStack> before = player.getInventory().items.stream().map(ItemStack::copy).toList();
        List<ItemStack> after = before.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (EconomySavedData.DepositSelection selection : reservation.selections()) {
            ItemStack actual = player.getInventory().items.get(selection.slot());
            var stamp = CoinProvenance.read(actual);
            if (stamp.isEmpty() || !stamp.get().lotId().equals(selection.lotId()) || stamp.get().denomination() != source
                    || actual.getCount() < selection.accepted()) {
                data.cancelExchange(requestId);
                return new ExchangeResult(Status.INVENTORY_CHANGED, 0L, 0);
            }
            after.get(selection.slot()).shrink(selection.accepted());
        }
        for (CoinLot lot : reservation.outputs()) {
            ItemStack output = CoinProvenance.issue(lot.denomination(), lot.issued(), lot.id(), lot.issuanceOperation());
            if (!insertSimulated(after, output)) {
                data.cancelExchange(requestId);
                return new ExchangeResult(Status.NO_SPACE, 0L, 0);
            }
        }
        for (int slot = 0; slot < after.size(); slot++) player.getInventory().items.set(slot, after.get(slot));
        if (!data.commitExchange(requestId)) {
            for (int slot = 0; slot < before.size(); slot++) player.getInventory().items.set(slot, before.get(slot));
            data.cancelExchange(requestId);
            return new ExchangeResult(Status.QUARANTINED, 0L, 0);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new ExchangeResult(Status.COMMITTED, reservation.value(), (int) (reservation.value() / target.value()));
    }

    private static boolean insertSimulated(List<ItemStack> inventory, ItemStack incoming) {
        for (ItemStack current : inventory) {
            if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, incoming) && current.getCount() < current.getMaxStackSize()) {
                int moved = Math.min(incoming.getCount(), current.getMaxStackSize() - current.getCount());
                current.grow(moved);
                incoming.shrink(moved);
                if (incoming.isEmpty()) return true;
            }
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            ItemStack placed = incoming.copy();
            placed.setCount(moved);
            inventory.set(slot, placed);
            incoming.shrink(moved);
            if (incoming.isEmpty()) return true;
        }
        return false;
    }

    public static ContainerPayment prepareContainerPayment(net.minecraft.server.MinecraftServer server, SystemAccount source,
                                                            long amount, String idempotencyKey) {
        if (!CurrencyAuthority.active(server) || idempotencyKey == null || idempotencyKey.isBlank()) {
            return new ContainerPayment(Status.UNAVAILABLE, null, 0L, List.of());
        }
        UUID operationId = UUID.nameUUIDFromBytes(("jem:container-payment:" + idempotencyKey).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        EconomySavedData.WithdrawLedgerResult plan = EconomySavedData.get(server).prepareSystemIssuance(source, amount, operationId);
        if (plan.status() == EconomySavedData.PhysicalStatus.INSUFFICIENT_FUNDS) return new ContainerPayment(Status.INSUFFICIENT_FUNDS, operationId, 0L, List.of());
        if (plan.status() == EconomySavedData.PhysicalStatus.REPLAYED) {
            List<ItemStack> replayStacks = plan.lots().stream().map(lot -> CoinProvenance.issue(lot.denomination(), lot.issued(), lot.id(), lot.issuanceOperation())).toList();
            return new ContainerPayment(Status.REPLAYED, operationId, plan.value(), replayStacks);
        }
        if (plan.status() != EconomySavedData.PhysicalStatus.PREPARED) return new ContainerPayment(Status.INVALID, operationId, 0L, List.of());
        List<ItemStack> stacks = plan.lots().stream().map(lot -> CoinProvenance.issue(lot.denomination(), lot.issued(), lot.id(), lot.issuanceOperation())).toList();
        return new ContainerPayment(Status.COMMITTED, operationId, amount, stacks);
    }

    public static boolean commitContainerPayment(net.minecraft.server.MinecraftServer server, UUID operationId) {
        return CurrencyAuthority.active(server) && operationId != null && EconomySavedData.get(server).commitWithdraw(operationId);
    }

    public static void quarantineContainerPayment(net.minecraft.server.MinecraftServer server, UUID operationId) {
        if (CurrencyAuthority.active(server) && operationId != null) EconomySavedData.get(server).quarantineWithdraw(operationId);
    }

    public static HeldSummary heldSummary(ServerPlayer player) {
        return heldSummary(player, player.getInventory().getContainerSize());
    }

    public static HeldSummary exchangeableSummary(ServerPlayer player) {
        return heldSummary(player, player.getInventory().items.size());
    }

    private static HeldSummary heldSummary(ServerPlayer player, int slotCount) {
        if (!CurrencyAuthority.active(player)) return new HeldSummary(0L, Map.of(), 0);
        EconomySavedData data = EconomySavedData.get(player.getServer());
        Map<CoinDenomination, Integer> counts = new EnumMap<>(CoinDenomination.class);
        long value = 0L;
        int invalid = 0;
        Map<UUID, Integer> seenByLot = new LinkedHashMap<>();
        for (int slot = 0; slot < slotCount; slot++) {
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
    public record ExchangeResult(Status status, long value, int targetItems) { }
    public record HeldSummary(long value, Map<CoinDenomination, Integer> counts, int invalidItems) { }
    public record ContainerPayment(Status status, UUID operationId, long value, List<ItemStack> stacks) { }
}
