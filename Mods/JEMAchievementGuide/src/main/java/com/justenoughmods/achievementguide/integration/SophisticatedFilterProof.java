package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.mixin.UpgradeWrapperQuestAccess;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;

/** Real native settings changes and native transfer/void results for the portable storage lesson. */
public final class SophisticatedFilterProof {
    private static final Map<IUpgradeWrapper, FilterComparisonProof.Configuration> CONFIGURED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<FilterLogic, WeakReference<IUpgradeWrapper>> FILTERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Transfer> TRANSFER = new ThreadLocal<>();
    static { MinecraftForge.EVENT_BUS.addListener(SophisticatedFilterProof::stopped); }
    public record Settings(Map<IUpgradeWrapper, CompoundTag> upgrades) {}
    private record Decision(IUpgradeWrapper wrapper, Object endpoint, ItemStack item, boolean allowed, long moved) {}
    private static final class NativeAttempt {
        final IItemHandler endpoint;
        final ItemStack offered;
        final boolean insertion;
        final boolean simulate;
        IUpgradeWrapper wrapper;
        long before = -1;
        boolean decided;
        boolean allowed;
        NativeAttempt(IItemHandler endpoint, ItemStack offered, boolean insertion, boolean simulate) {
            this.endpoint = endpoint; this.offered = offered.copy(); this.insertion = insertion; this.simulate = simulate;
        }
    }
    private static final class Transfer {
        final ServerPlayer actor;
        final IItemHandler source;
        final IItemHandler destination;
        final Map<ItemTransferObservation.Kind, Long> from;
        final Map<ItemTransferObservation.Kind, Long> to;
        final List<Decision> decisions = new ArrayList<>();
        final List<NativeAttempt> nativeCalls = new ArrayList<>();
        int inspected;
        boolean ambiguous;
        Transfer(ServerPlayer actor, IItemHandler source, IItemHandler destination) {
            this.actor = actor; this.source = source; this.destination = destination;
            from = FilterComparisonProof.snapshot(source); to = FilterComparisonProof.snapshot(destination);
        }
    }
    public record PendingVoid(FilterComparisonProof.Configuration configuration, ItemStack item) {}
    private SophisticatedFilterProof() {}
    private static void stopped(ServerStoppedEvent event) { CONFIGURED.clear(); FILTERS.clear(); TRANSFER.remove(); }

    public static Settings before(StorageContainerMenuBase<?> menu, Player player) {
        Map<IUpgradeWrapper, CompoundTag> upgrades = new HashMap<>();
        if (!(player instanceof ServerPlayer actor) || !FilterComparisonProof.real(actor)
                || actor.containerMenu != menu || !menu.stillValid(actor) || menu.getUpgradeContainers().size() > 64) return new Settings(upgrades);
        for (var container : menu.getUpgradeContainers().values()) {
            IUpgradeWrapper wrapper = container.getUpgradeWrapper();
            if (wrapper instanceof FilterUpgradeWrapper || wrapper instanceof VoidUpgradeWrapper) upgrades.put(wrapper, settings(wrapper));
        }
        return new Settings(upgrades);
    }
    public static void configured(StorageContainerMenuBase<?> menu, Player player, Settings before) {
        if (before == null || !(player instanceof ServerPlayer actor) || !FilterComparisonProof.real(actor)
                || actor.containerMenu != menu || !menu.stillValid(actor)) return;
        for (var row : before.upgrades.entrySet()) {
            IUpgradeWrapper wrapper = row.getKey();
            CompoundTag now = settings(wrapper);
            if (now.equals(row.getValue()) || !installed(wrapper)) continue;
            if (CONFIGURED.size() >= 256) CONFIGURED.clear();
            if (FILTERS.size() >= 256) FILTERS.clear();
            CONFIGURED.put(wrapper, new FilterComparisonProof.Configuration(actor, now));
            FilterLogic filter = wrapper instanceof FilterUpgradeWrapper transfer ? transfer.getFilterLogic() : ((VoidUpgradeWrapper)wrapper).getFilterLogic();
            FILTERS.put(filter, new WeakReference<>(wrapper));
        }
    }
    public static CompoundTag settings(IUpgradeWrapper wrapper) { return wrapper.getUpgradeStack().save(new CompoundTag()); }
    private static boolean installed(IUpgradeWrapper wrapper) {
        var storage = ((UpgradeWrapperQuestAccess)wrapper).jem$questStorageWrapper();
        return wrapper.isEnabled() && storage.getUpgradeHandler().getSlotWrappers().size() <= 64
                && storage.getUpgradeHandler().getSlotWrappers().containsValue(wrapper);
    }
    public static List<ItemStack> transfer(Object wrapper, IItemHandler external, Player player, boolean deposit, Supplier<List<ItemStack>> nativeAction) {
        Transfer previous = TRANSFER.get();
        TRANSFER.remove();
        if (player instanceof ServerPlayer actor && FilterComparisonProof.real(actor) && ((IUpgradeWrapper)wrapper).isEnabled()) {
            var inventory = ((UpgradeWrapperQuestAccess)wrapper).jem$questStorageWrapper().getInventoryForUpgradeProcessing();
            if (inventory != external && inventory.getSlots() <= FilterComparisonProof.SLOT_LIMIT && external.getSlots() <= FilterComparisonProof.SLOT_LIMIT) {
                TRANSFER.set(new Transfer(actor, deposit ? inventory : external, deposit ? external : inventory));
            }
        }
        try {
            List<ItemStack> result = nativeAction.get();
            finished(TRANSFER.get());
            return result;
        } finally {
            if (previous == null) TRANSFER.remove(); else TRANSFER.set(previous);
        }
    }
    public static void decision(Object endpoint, ItemStack item, List<FilterLogic> filters, boolean allowed) {
        Transfer transfer = TRANSFER.get();
        if (transfer == null || transfer.ambiguous || transfer.from == null || transfer.to == null || transfer.nativeCalls.isEmpty()) return;
        NativeAttempt attempt = transfer.nativeCalls.get(transfer.nativeCalls.size() - 1);
        if (attempt.endpoint != endpoint || attempt.decided || !ItemStack.isSameItemSameTags(attempt.offered, item)) return;
        attempt.decided = true;
        if (item.isEmpty() || filters.size() != 1 || transfer.inspected++ >= 128) return;
        WeakReference<IUpgradeWrapper> reference = FILTERS.get(filters.get(0));
        IUpgradeWrapper wrapper = reference == null ? null : reference.get();
        var configuration = CONFIGURED.get(wrapper);
        if (wrapper instanceof FilterUpgradeWrapper && installed(wrapper) && configuration != null
                && configuration.valid(transfer.actor, settings(wrapper))) {
            attempt.before = FilterComparisonProof.count(attempt.endpoint, attempt.offered);
            attempt.wrapper = wrapper;
            attempt.allowed = allowed;
        }
    }
    /** Brackets the actual native endpoint method, including the Modifiable overrides. */
    public static void beginEndpoint(IItemHandler endpoint, ItemStack offered, boolean insertion, boolean simulate) {
        Transfer transfer = TRANSFER.get();
        if (transfer == null || transfer.ambiguous) return;
        if (transfer.nativeCalls.size() >= 16) { transfer.ambiguous = true; return; }
        transfer.nativeCalls.add(new NativeAttempt(endpoint, offered, insertion, simulate));
    }
    public static void beginExtraction(IItemHandler endpoint, int slot, boolean simulate) {
        if (TRANSFER.get() == null) return;
        ItemStack offered = slot >= 0 && slot < endpoint.getSlots() && endpoint.getSlots() <= FilterComparisonProof.SLOT_LIMIT
                ? endpoint.getStackInSlot(slot) : ItemStack.EMPTY;
        beginEndpoint(endpoint, offered, false, simulate);
    }
    public static void endEndpoint(IItemHandler endpoint, ItemStack result) {
        Transfer transfer = TRANSFER.get();
        if (transfer == null || transfer.ambiguous) return;
        if (transfer.nativeCalls.isEmpty()) { transfer.ambiguous = true; return; }
        NativeAttempt attempt = transfer.nativeCalls.remove(transfer.nativeCalls.size() - 1);
        if (attempt.endpoint != endpoint) { transfer.ambiguous = true; return; }
        if (attempt.before < 0 || attempt.wrapper == null || !attempt.decided || transfer.decisions.size() >= 128
                || !result.isEmpty() && !ItemStack.isSameItemSameTags(result, attempt.offered)) return;
        long after = FilterComparisonProof.count(endpoint, attempt.offered);
        if (after < 0) return;
        long moved = attempt.insertion ? attempt.offered.getCount() - result.getCount() : result.getCount();
        long endpointDelta = attempt.insertion ? after - attempt.before : attempt.before - after;
        boolean proved = attempt.allowed ? !attempt.simulate && moved > 0 && endpointDelta == moved
                : moved == 0 && endpointDelta == 0;
        if (proved) transfer.decisions.add(new Decision(attempt.wrapper, endpoint, attempt.offered.copyWithCount(1), attempt.allowed, moved));
    }
    private static void finished(Transfer transfer) {
        if (transfer == null || transfer.ambiguous || !transfer.nativeCalls.isEmpty() || transfer.from == null || transfer.to == null) return;
        var from = FilterComparisonProof.snapshot(transfer.source);
        var to = FilterComparisonProof.snapshot(transfer.destination);
        if (from == null || to == null) return;
        for (Decision decision : transfer.decisions) {
            var kind = FilterComparisonProof.kind(decision.item);
            long debit = transfer.from.getOrDefault(kind, 0L) - from.getOrDefault(kind, 0L);
            long credit = to.getOrDefault(kind, 0L) - transfer.to.getOrDefault(kind, 0L);
            boolean proved = decision.allowed ? debit > 0 && debit == credit && decision.moved > 0 && decision.moved <= debit
                    : transfer.from.getOrDefault(kind, 0L) > 0 && debit == 0 && credit == 0;
            var configuration = CONFIGURED.get(decision.wrapper);
            if (proved && configuration != null && installed(decision.wrapper)) configuration.observed(transfer.actor,
                    settings(decision.wrapper), decision.endpoint, decision.item, decision.allowed, "storage_filter_comparison");
        }
    }
    public static PendingVoid queued(VoidUpgradeWrapper wrapper, IItemHandler inventory, int slot, ItemStack item, boolean matched) {
        var configuration = CONFIGURED.get(wrapper);
        ServerPlayer actor = configuration == null ? null : configuration.actor.get();
        if (!matched || actor == null || !installed(wrapper) || !configuration.valid(actor, settings(wrapper))
                || inventory != ((UpgradeWrapperQuestAccess)wrapper).jem$questStorageWrapper().getInventoryHandler()
                || slot < 0 || slot >= inventory.getSlots() || inventory.getSlots() > FilterComparisonProof.SLOT_LIMIT
                || !disposable(item) || !wrapper.getFilterLogic().isAllowList()
                || wrapper.getFilterLogic().getPrimaryMatch() != PrimaryMatch.ITEM) return null;
        IItemHandler filters = wrapper.getFilterLogic().getFilterHandler();
        if (filters.getSlots() > 64) return null;
        int exemplars = 0;
        for (int i = 0; i < filters.getSlots(); i++) {
            ItemStack exemplar = filters.getStackInSlot(i);
            if (!exemplar.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(exemplar, item)) return null;
                exemplars++;
            }
        }
        return exemplars == 1 ? new PendingVoid(configuration, item.copy()) : null;
    }
    public static void voided(VoidUpgradeWrapper wrapper, IItemHandler inventory, int slot, ItemStack before, ItemStack removed, PendingVoid pending) {
        if (pending == null || removed.isEmpty() || !ItemStack.isSameItemSameTags(before, pending.item)
                || !ItemStack.isSameItemSameTags(removed, pending.item) || !installed(wrapper)
                || CONFIGURED.get(wrapper) != pending.configuration) return;
        ServerPlayer actor = pending.configuration.actor.get();
        ItemStack after = inventory.getStackInSlot(slot);
        int remaining = after.isEmpty() ? 0 : ItemStack.isSameItemSameTags(before, after) ? after.getCount() : -1;
        if (actor != null && pending.configuration.valid(actor, settings(wrapper)) && remaining >= 0
                && removed.getCount() > 0 && before.getCount() - remaining == removed.getCount()) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_store_filter_void");
        }
    }
    private static boolean disposable(ItemStack stack) {
        return !stack.isEmpty() && !stack.hasTag() && (stack.is(Items.COBBLESTONE) || stack.is(Items.DIRT) || stack.is(Items.NETHERRACK));
    }
}
