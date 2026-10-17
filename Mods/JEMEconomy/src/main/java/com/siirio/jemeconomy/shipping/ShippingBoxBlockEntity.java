package com.siirio.jemeconomy.shipping;

import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.full.market.MarketRuntime;
import com.siirio.jemeconomy.market.MarketSavedData;
import com.siirio.jemeconomy.physical.CoinProvenance;
import com.siirio.jemeconomy.physical.PhysicalCoinService;
import com.siirio.jemeconomy.policy.ItemPolicy;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemeconomy.registry.EconomyBlockEntities;
import com.siirio.jemeconomy.registry.EconomyItems;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class ShippingBoxBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_SLOTS = 54;
    public static final int PAYOUT_START = 54;
    public static final int TOTAL_SLOTS = 63;
    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private UUID owner;
    private String ownerName = "";
    private UUID instanceId = UUID.randomUUID();
    private long lastProcessedCycle = Long.MIN_VALUE;
    private long sequence;
    private String status = "waiting";
    private long previewPayout;
    private Transaction transaction;
    private final int[] synced = new int[8];
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            if (!(level instanceof ServerLevel serverLevel) || owner == null) return synced[index];
            MarketSavedData market = MarketSavedData.get(serverLevel.getServer()); var demand = market.demand(serverLevel.getServer());
            long time = serverLevel.getServer().overworld().getDayTime(); long next = (Math.floorDiv(time - 1_000L, 24_000L) + 1L) * 24_000L + 1_000L;
            return switch (index) { case 0 -> statusCode(); case 1 -> (int) Math.min(Integer.MAX_VALUE, previewPayout); case 2 -> (int) market.accountRemaining(serverLevel.getServer(), owner);
                case 3 -> (int) market.globalRemaining(serverLevel.getServer()); case 4 -> (int) Math.max(0L, next - time);
                case 5 -> groupIndex(demand.primary()); case 6 -> groupIndex(demand.secondary()); case 7 -> transaction == null ? 0 : transaction.phase.ordinal() + 1; default -> 0; };
        }
        @Override public void set(int index, int value) { synced[index] = value; }
        @Override public int getCount() { return synced.length; }
    };

    public ShippingBoxBlockEntity(BlockPos pos, BlockState state) { super(EconomyBlockEntities.SHIPPING_BOX.get(), pos, state); }

    public void bindOwner(UUID account, String name) {
        if (owner == null) {
            owner = account; ownerName = name == null ? "" : name;
            if (level instanceof ServerLevel serverLevel) lastProcessedCycle = MarketSavedData.get(serverLevel.getServer()).cycle(serverLevel.getServer());
            setChanged();
        }
    }
    public boolean isOwner(Player player) {
        return player instanceof ServerPlayer serverPlayer && owner != null && IdentityPolicy.resolve(serverPlayer).map(principal -> owner.equals(principal.accountId())).orElse(false);
    }
    public String ownerName() { return ownerName; }
    public String status() { return status; }
    public ContainerData data() { return data; }
    public int inputComparatorSignal() {
        float fullness = 0.0F; int occupied = 0;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) { ItemStack stack = items.get(slot); if (stack.isEmpty()) continue; fullness += (float) stack.getCount() / Math.min(getMaxStackSize(), stack.getMaxStackSize()); occupied++; }
        return net.minecraft.util.Mth.floor(fullness / INPUT_SLOTS * 14.0F) + (occupied > 0 ? 1 : 0);
    }

    @Override public Component getDisplayName() { return Component.translatable("container.jem_economy.shipping_box"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return isOwner(player) ? new ShippingBoxMenu(id, inventory, this) : null; }

    @Override public void onLoad() { super.onLoad(); if (level instanceof ServerLevel serverLevel) MarketRuntime.register(serverLevel, worldPosition); }
    @Override public void setRemoved() { if (level instanceof ServerLevel serverLevel) MarketRuntime.unregister(serverLevel, worldPosition); super.setRemoved(); }

    public void processScheduled(long cycle) {
        if (!(level instanceof ServerLevel serverLevel) || owner == null) return;
        if (transaction != null && transaction.phase != Phase.COMMITTED && transaction.phase != Phase.QUARANTINED) recover(serverLevel);
        if (transaction != null && transaction.phase == Phase.QUARANTINED) { status = "quarantined"; return; }
        if (cycle <= lastProcessedCycle) return;
        lastProcessedCycle = cycle;
        process(serverLevel, cycle);
        setChanged();
    }

    public void preview() {
        if (!(level instanceof ServerLevel serverLevel) || owner == null) return;
        Quote quote = quote(serverLevel);
        previewPayout = quote.payout;
        status = quote.payout > 0L ? "preview:" + quote.payout : quote.reason;
        setChanged();
    }

    private void process(ServerLevel serverLevel, long cycle) {
        Quote quote = quote(serverLevel);
        previewPayout = quote.payout;
        if (quote.payout <= 0L) { status = quote.reason; return; }
        int emptyPayout = 0;
        for (int slot = PAYOUT_START; slot < TOTAL_SLOTS; slot++) if (items.get(slot).isEmpty()) emptyPayout++;
        int required = PhysicalCoinService.decompose(quote.payout).values().stream().mapToInt(value -> (int) ((value + 63L) / 64L)).sum();
        if (emptyPayout < required) { status = "payout_full"; return; }
        String dimension = serverLevel.dimension().location().toString();
        String key = "shipping:" + dimension + ":" + worldPosition.asLong() + ":" + instanceId + ":" + cycle + ":" + sequence++;
        transaction = new Transaction(key, cycle, Phase.PREPARED, quote.payout, null, quote.lines);
        setChanged();
        recover(serverLevel);
    }

    private void recover(ServerLevel serverLevel) {
        if (transaction == null) return;
        try {
            if (transaction.phase == Phase.PREPARED) {
                if (!fingerprintsMatch(transaction.lines)) { quarantine(serverLevel, "inventory_changed"); return; }
                var payment = PhysicalCoinService.prepareContainerPayment(serverLevel.getServer(), SystemAccount.NPC_MARKET, transaction.payout, transaction.key);
                if (payment.status() == PhysicalCoinService.Status.INSUFFICIENT_FUNDS) { transaction = null; status = "market_budget_exhausted"; setChanged(); return; }
                if (payment.status() != PhysicalCoinService.Status.COMMITTED && payment.status() != PhysicalCoinService.Status.REPLAYED) { quarantine(serverLevel, "issuance_failed"); return; }
                transaction = transaction.with(Phase.FUNDS_RESERVED, payment.operationId()); setChanged();
            }
            if (transaction.phase == Phase.FUNDS_RESERVED) {
                if (!fingerprintsMatch(transaction.lines)) { quarantine(serverLevel, "inventory_changed"); return; }
                for (Line line : transaction.lines) items.get(line.slot).shrink(line.count);
                transaction = transaction.with(Phase.INPUT_CONSUMED, transaction.operationId); setChanged();
            }
            if (transaction.phase == Phase.INPUT_CONSUMED) {
                var payment = PhysicalCoinService.prepareContainerPayment(serverLevel.getServer(), SystemAccount.NPC_MARKET, transaction.payout, transaction.key);
                if (payment.status() != PhysicalCoinService.Status.REPLAYED && payment.status() != PhysicalCoinService.Status.COMMITTED) { quarantine(serverLevel, "issuance_recovery_failed"); return; }
                int cursor = PAYOUT_START;
                for (ItemStack stack : payment.stacks()) {
                    var stamp = CoinProvenance.read(stack); if (stamp.isEmpty()) { quarantine(serverLevel, "unstamped_payment"); return; }
                    if (containsLot(stamp.get().lotId())) continue;
                    while (cursor < TOTAL_SLOTS && !items.get(cursor).isEmpty()) cursor++;
                    if (cursor >= TOTAL_SLOTS) { quarantine(serverLevel, "payout_full_during_recovery"); return; }
                    items.set(cursor++, stack.copy());
                }
                transaction = transaction.with(Phase.PAYOUT_ISSUED, transaction.operationId); setChanged();
            }
            if (transaction.phase == Phase.PAYOUT_ISSUED) {
                if (!PhysicalCoinService.commitContainerPayment(serverLevel.getServer(), transaction.operationId)) { quarantine(serverLevel, "ledger_commit_failed"); return; }
                Map<ResourceLocation, Integer> units = new HashMap<>(); transaction.lines.forEach(line -> units.merge(line.item, line.count, Integer::sum));
                if (!MarketSavedData.get(serverLevel.getServer()).commit(serverLevel.getServer(), transaction.key, owner, units, transaction.payout)) {
                    quarantine(serverLevel, "quota_commit_failed"); return;
                }
                transaction = transaction.with(Phase.COMMITTED, transaction.operationId); status = "paid:" + transaction.payout; setChanged();
            }
        } catch (RuntimeException failure) { quarantine(serverLevel, "exception"); }
    }

    private void quarantine(ServerLevel level, String reason) {
        if (transaction != null && transaction.operationId != null) PhysicalCoinService.quarantineContainerPayment(level.getServer(), transaction.operationId);
        if (transaction != null) transaction = transaction.with(Phase.QUARANTINED, transaction.operationId);
        status = "quarantined:" + reason; setChanged();
    }

    private Quote quote(ServerLevel level) {
        MarketSavedData market = MarketSavedData.get(level.getServer());
        long remaining = Math.min(market.accountRemaining(level.getServer(), owner), market.globalRemaining(level.getServer()));
        List<Line> lines = new ArrayList<>();
        Map<ResourceLocation, Integer> localUnits = new HashMap<>();
        long payout = 0L;
        String reason = "empty";
        for (int slot = 0; slot < INPUT_SLOTS && remaining > 0L; slot++) {
            ItemStack stack = items.get(slot); if (stack.isEmpty()) continue;
            var optional = ItemPolicyRegistry.policy(stack); if (optional.isEmpty() || !optional.get().sellable()) { reason = "blocked_item"; continue; }
            ItemPolicy policy = optional.get();
            long unit = Math.max(1L, Math.floorDiv(Math.multiplyExact(policy.sellCopper(), market.multiplier(level.getServer(), policy.demandGroup())), 100L));
            int requested = stack.getCount();
            MarketSavedData.QuoteAllowance allowance = market.allowance(level.getServer(), owner, policy, requested, unit);
            int already = localUnits.getOrDefault(policy.item(), 0);
            int units = Math.min(allowance.units(), Math.max(0, policy.dailyPlayerUnits() - already));
            units = (int) Math.min(units, remaining / unit);
            if (units <= 0) { reason = allowance.reason(); continue; }
            lines.add(new Line(slot, policy.item(), units, stack.copyWithCount(units)));
            localUnits.merge(policy.item(), units, Integer::sum);
            long value = Math.multiplyExact((long) units, unit); payout = Math.addExact(payout, value); remaining -= value; reason = "ok";
        }
        return new Quote(List.copyOf(lines), payout, reason);
    }

    private boolean fingerprintsMatch(List<Line> lines) {
        for (Line line : lines) { ItemStack current = items.get(line.slot); if (current.getCount() < line.count || !ItemStack.isSameItemSameTags(current, line.fingerprint)) return false; }
        return true;
    }
    private boolean containsLot(UUID lot) {
        if (lot == null) return false;
        for (int slot = PAYOUT_START; slot < TOTAL_SLOTS; slot++) if (CoinProvenance.read(items.get(slot)).map(stamp -> lot.equals(stamp.lotId())).orElse(false)) return true;
        return false;
    }

    public void collect(ServerPlayer player) {
        if (!isOwner(player)) return;
        for (int slot = PAYOUT_START; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.get(slot); if (stack.isEmpty()) continue;
            ItemStack copy = stack.copy(); player.getInventory().add(copy); items.set(slot, copy.isEmpty() ? ItemStack.EMPTY : copy);
        }
        player.getInventory().setChanged(); setChanged();
    }

    public ItemStack preservedStack() {
        ItemStack stack = new ItemStack(EconomyItems.SHIPPING_BOX.get()); CompoundTag root = new CompoundTag(); saveAdditional(root);
        stack.getOrCreateTag().put("BlockEntityTag", root); return stack;
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); ContainerHelper.saveAllItems(tag, items); if (owner != null) tag.putUUID("Owner", owner); tag.putString("OwnerName", ownerName);
        tag.putUUID("Instance", instanceId); tag.putLong("LastCycle", lastProcessedCycle); tag.putLong("Sequence", sequence); tag.putString("Status", status); tag.putLong("PreviewPayout", previewPayout);
        if (transaction != null) tag.put("Transaction", transaction.save());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); ContainerHelper.loadAllItems(tag, items); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; ownerName = tag.getString("OwnerName");
        instanceId = tag.hasUUID("Instance") ? tag.getUUID("Instance") : UUID.randomUUID(); lastProcessedCycle = tag.contains("LastCycle") ? tag.getLong("LastCycle") : Long.MIN_VALUE;
        sequence = tag.getLong("Sequence"); status = tag.getString("Status"); previewPayout = tag.getLong("PreviewPayout"); transaction = tag.contains("Transaction", Tag.TAG_COMPOUND) ? Transaction.load(tag.getCompound("Transaction")) : null;
    }

    @Override public int getContainerSize() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack result = ContainerHelper.removeItem(items, slot, amount); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (slot < INPUT_SLOTS && !stack.isEmpty() && !ItemPolicyRegistry.acceptsShipping(stack)) return; if (slot >= PAYOUT_START && !stack.isEmpty() && CoinProvenance.read(stack).isEmpty()) return; items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return isOwner(player) && level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) <= 64D; }
    @Override public void clearContent() { for (int slot = 0; slot < items.size(); slot++) items.set(slot, ItemStack.EMPTY); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < INPUT_SLOTS && ItemPolicyRegistry.acceptsShipping(stack); }

    private int statusCode() {
        String key = status; int split = key.indexOf(':'); if (split >= 0) key = key.substring(0, split);
        return switch (key) { case "preview" -> 1; case "paid" -> 2; case "empty" -> 3; case "blocked_item" -> 4; case "account_daily_cap" -> 5;
            case "global_daily_cap" -> 6; case "item_account_cap" -> 7; case "item_global_cap" -> 8; case "budget_too_small" -> 9; case "payout_full" -> 10;
            case "market_budget_exhausted" -> 11; case "quarantined" -> 12; default -> 0; };
    }
    private static int groupIndex(String group) { return switch (group) { case "crops" -> 0; case "fishing" -> 1; case "cooking" -> 2; case "animal_products" -> 3; case "mining" -> 4; case "forestry" -> 5; default -> 6; }; }

    private enum Phase { PREPARED, FUNDS_RESERVED, INPUT_CONSUMED, PAYOUT_ISSUED, COMMITTED, QUARANTINED }
    private record Quote(List<Line> lines, long payout, String reason) { }
    private record Line(int slot, ResourceLocation item, int count, ItemStack fingerprint) {
        CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putInt("Slot", slot); tag.putString("Item", item.toString()); tag.putInt("Count", count); tag.put("Fingerprint", fingerprint.save(new CompoundTag())); return tag; }
        static Line load(CompoundTag tag) { return new Line(tag.getInt("Slot"), ResourceLocation.tryParse(tag.getString("Item")), tag.getInt("Count"), ItemStack.of(tag.getCompound("Fingerprint"))); }
    }
    private record Transaction(String key, long cycle, Phase phase, long payout, UUID operationId, List<Line> lines) {
        Transaction with(Phase next, UUID operation) { return new Transaction(key, cycle, next, payout, operation, lines); }
        CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putString("Key", key); tag.putLong("Cycle", cycle); tag.putString("Phase", phase.name()); tag.putLong("Payout", payout); if (operationId != null) tag.putUUID("Operation", operationId); ListTag list = new ListTag(); lines.forEach(line -> list.add(line.save())); tag.put("Lines", list); return tag; }
        static Transaction load(CompoundTag tag) { List<Line> lines = new ArrayList<>(); ListTag list = tag.getList("Lines", Tag.TAG_COMPOUND); for (int i = 0; i < list.size(); i++) lines.add(Line.load(list.getCompound(i))); Phase phase; try { phase = Phase.valueOf(tag.getString("Phase")); } catch (IllegalArgumentException ignored) { phase = Phase.QUARANTINED; } return new Transaction(tag.getString("Key"), tag.getLong("Cycle"), phase, tag.getLong("Payout"), tag.hasUUID("Operation") ? tag.getUUID("Operation") : null, List.copyOf(lines)); }
    }
}
