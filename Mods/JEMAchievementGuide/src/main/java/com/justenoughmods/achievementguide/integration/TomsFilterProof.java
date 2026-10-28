package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.mixin.TomsFilterMenuQuestAccess;
import com.justenoughmods.achievementguide.mixin.TomsTerminalPlayerQuestAccess;
import com.tom.storagemod.gui.InventoryConnectorFilterMenu;
import com.tom.storagemod.gui.StorageTerminalMenu;
import com.tom.storagemod.tile.FilteredInventoryCableConnectorBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Native terminal deposits through the same endpoint the real actor actually configured. */
public final class TomsFilterProof {
    private static final Map<FilteredInventoryCableConnectorBlockEntity, FilterComparisonProof.Configuration> CONFIGURED = new WeakHashMap<>();
    private static final ThreadLocal<Run> RUN = new ThreadLocal<>();
    static {
        MinecraftForge.EVENT_BUS.addListener(TomsFilterProof::stopped);
        MinecraftForge.EVENT_BUS.addListener(TomsFilterProof::loggedOut);
    }
    private record Observation(FilteredInventoryCableConnectorBlockEntity endpoint, FilterComparisonProof.Configuration configuration,
                               ItemStack item, long credit, boolean allowed) {}
    private static final class Run {
        final StorageTerminalMenu menu;
        final ServerPlayer actor;
        final IItemHandler network;
        final long tick;
        final Map<ItemTransferObservation.Kind, Long> held;
        final List<Observation> observations = new ArrayList<>();
        int depth;
        int inspected;
        Run(StorageTerminalMenu menu, ServerPlayer actor, IItemHandler network) {
            this.menu = menu; this.actor = actor; this.network = network; tick = actor.level().getGameTime(); held = held(actor, menu);
        }
    }
    public static final class Insertion {
        final Run run;
        final FilteredInventoryCableConnectorBlockEntity endpoint;
        final FilterComparisonProof.Configuration configuration;
        final IItemHandler destination;
        final ItemStack offered;
        final long before;
        boolean allowed;
        boolean decided;
        Insertion(Run run, FilteredInventoryCableConnectorBlockEntity endpoint, FilterComparisonProof.Configuration configuration,
                  IItemHandler destination, ItemStack offered, long before) {
            this.run = run; this.endpoint = endpoint; this.configuration = configuration;
            this.destination = destination; this.offered = offered; this.before = before;
        }
    }
    private TomsFilterProof() {}
    private static void stopped(ServerStoppedEvent event) { CONFIGURED.clear(); RUN.remove(); }
    private static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Run run = RUN.get();
        if (run != null && run.actor == event.getEntity()) RUN.remove();
    }
    public static CompoundTag beforeConfiguration(AbstractContainerMenu menu, Player player) {
        if (!(menu instanceof InventoryConnectorFilterMenu) || !(player instanceof ServerPlayer actor)
                || !FilterComparisonProof.real(actor) || actor.containerMenu != menu || !menu.stillValid(actor)) return null;
        var endpoint = ((TomsFilterMenuQuestAccess)menu).jem$nativeFilterEndpoint();
        return usable(endpoint, actor) ? settings(endpoint) : null;
    }
    public static void configured(AbstractContainerMenu menu, Player player, CompoundTag before) {
        if (before == null || !(player instanceof ServerPlayer actor)) return;
        CompoundTag after = beforeConfiguration(menu, player);
        if (after == null || before.equals(after)) return;
        var endpoint = ((TomsFilterMenuQuestAccess)menu).jem$nativeFilterEndpoint();
        if (CONFIGURED.size() >= 256) CONFIGURED.clear();
        CONFIGURED.put(endpoint, new FilterComparisonProof.Configuration(actor, after));
    }
    private static CompoundTag settings(FilteredInventoryCableConnectorBlockEntity endpoint) {
        CompoundTag settings = new CompoundTag();
        settings.putBoolean("allow", endpoint.isAllowList());
        settings.putBoolean("keep", endpoint.isKeepLastInSlot());
        settings.putInt("priority", endpoint.getPriority().ordinal());
        settings.putString("state", endpoint.getBlockState().toString());
        ListTag filters = new ListTag();
        for (int slot = 0; slot < 9; slot++) filters.add(endpoint.getFilter().getItem(slot).save(new CompoundTag()));
        settings.put("filter", filters);
        return settings;
    }
    private static boolean usable(FilteredInventoryCableConnectorBlockEntity endpoint, ServerPlayer actor) {
        return endpoint != null && !endpoint.isRemoved() && endpoint.getLevel() == actor.level()
                && actor.level().hasChunkAt(endpoint.getBlockPos()) && actor.level().getBlockEntity(endpoint.getBlockPos()) == endpoint;
    }
    public static void begin(StorageTerminalMenu menu) {
        if (!(((TomsTerminalPlayerQuestAccess)menu).jem$nativeTerminalPlayerInventory().player instanceof ServerPlayer actor)
                || !FilterComparisonProof.real(actor) || actor.containerMenu != menu || !menu.stillValid(actor)
                || menu.getTerminal() == null || menu.getTerminal().getLevel() != actor.level() || !menu.getTerminal().canInteractWith(actor)) { RUN.remove(); return; }
        Run run = RUN.get();
        if (run == null || run.menu != menu || run.tick != actor.level().getGameTime()) {
            IItemHandler network = ((StorageNetworkAccess)menu.getTerminal()).jem$questStorageNetwork();
            if (network == null || network.getSlots() < 1 || network.getSlots() > FilterComparisonProof.SLOT_LIMIT) { RUN.remove(); return; }
            run = new Run(menu, actor, network); RUN.set(run);
        }
        run.depth++;
    }
    public static void end(StorageTerminalMenu menu) {
        Run run = RUN.get();
        if (run == null || run.menu != menu || --run.depth > 0) return;
        RUN.remove();
        if (run.tick != run.actor.level().getGameTime() || run.actor.containerMenu != menu || !FilterComparisonProof.real(run.actor)
                || !menu.stillValid(run.actor) || menu.getTerminal() == null || !menu.getTerminal().canInteractWith(run.actor)
                || ((StorageNetworkAccess)menu.getTerminal()).jem$questStorageNetwork() != run.network) return;
        var after = held(run.actor, menu);
        Map<ItemTransferObservation.Kind, Long> credited = new HashMap<>();
        for (Observation observation : run.observations) if (observation.allowed) credited.merge(FilterComparisonProof.kind(observation.item), observation.credit, Long::sum);
        for (Observation observation : run.observations) {
            var kind = FilterComparisonProof.kind(observation.item);
            long debit = run.held.getOrDefault(kind, 0L) - after.getOrDefault(kind, 0L);
            boolean proved = observation.allowed ? debit > 0 && debit == credited.getOrDefault(kind, 0L)
                    : run.held.getOrDefault(kind, 0L) > 0 && debit == 0;
            if (proved && usable(observation.endpoint, run.actor) && CONFIGURED.get(observation.endpoint) == observation.configuration) {
                observation.configuration.observed(run.actor, settings(observation.endpoint), observation.endpoint,
                        observation.item, observation.allowed, "v3_store_network_filter");
            }
        }
    }
    public static Insertion beforeInsert(FilteredInventoryCableConnectorBlockEntity endpoint, IItemHandler destination, ItemStack item, boolean simulate) {
        Run run = RUN.get();
        if (run == null || simulate || item.isEmpty() || run.inspected++ >= 64 || !usable(endpoint, run.actor)) return null;
        var configuration = CONFIGURED.get(endpoint);
        if (configuration == null || !configuration.valid(run.actor, settings(endpoint))) return null;
        long before = FilterComparisonProof.count(destination, item);
        return before < 0 ? null : new Insertion(run, endpoint, configuration, destination, item.copy(), before);
    }
    public static void decision(Insertion insertion, boolean allowed) {
        if (insertion != null) { insertion.allowed = allowed; insertion.decided = true; }
    }
    public static void inserted(Insertion insertion, ItemStack remaining) {
        if (insertion == null || insertion.run != RUN.get() || !insertion.decided
                || !remaining.isEmpty() && !ItemStack.isSameItemSameTags(remaining, insertion.offered)) return;
        long after = FilterComparisonProof.count(insertion.destination, insertion.offered);
        long accepted = insertion.offered.getCount() - remaining.getCount();
        boolean proved = insertion.allowed ? accepted > 0 && after - insertion.before == accepted
                : accepted == 0 && after == insertion.before;
        if (proved) insertion.run.observations.add(new Observation(insertion.endpoint, insertion.configuration, insertion.offered.copyWithCount(1), accepted, insertion.allowed));
    }
    private static Map<ItemTransferObservation.Kind, Long> held(ServerPlayer actor, AbstractContainerMenu menu) {
        Map<ItemTransferObservation.Kind, Long> held = new HashMap<>();
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
            ItemStack stack = actor.getInventory().getItem(slot);
            if (!stack.isEmpty()) held.merge(FilterComparisonProof.kind(stack), (long)stack.getCount(), Long::sum);
        }
        if (!menu.getCarried().isEmpty()) held.merge(FilterComparisonProof.kind(menu.getCarried()), (long)menu.getCarried().getCount(), Long::sum);
        return held;
    }
}
