package com.justenoughmods.achievementguide.integration.create;

import com.google.common.collect.Multimap;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** One observed native recipe-mode gauge invocation and its exact native request vector. */
public final class FactoryRecipeProof {
    private static final String CONFIG = "jemFactoryRecipeConfigV4_";
    private static final ThreadLocal<Cycle> CURRENT = new ThreadLocal<>();
    private record Group(PackageOrderWithCrafts order, Multimap<PackagerBlockEntity, PackagingRequest> requests) {}
    private static final class Cycle {
        final FactoryPanelBehaviour panel;
        final ServerPlayer actor;
        final UUID identity = UUID.randomUUID();
        final CompoundTag configuration;
        final Map<Multimap<PackagerBlockEntity, PackagingRequest>, Group> groups = new IdentityHashMap<>();
        final List<ItemStack> parcels = new ArrayList<>();
        final List<Multimap<PackagerBlockEntity, PackagingRequest>> performed = new ArrayList<>();
        boolean valid = true;
        Cycle(FactoryPanelBehaviour panel, ServerPlayer actor, CompoundTag configuration) {
            this.panel = panel; this.actor = actor; this.configuration = configuration;
        }
    }
    private FactoryRecipeProof() {}
    private static String key(FactoryPanelBehaviour panel) { return CONFIG + panel.slot.name(); }

    public static CompoundTag configuration(FactoryPanelBehaviour panel) {
        CompoundTag config = CreateGaugeProofs.configuration(panel);
        if (panel.targetedBy.size() > 32 || panel.activeCraftingArrangement.size() > 1024) return new CompoundTag();
        config.putInt("recipe_output", panel.recipeOutput);
        config.put("product", panel.getFilter().copyWithCount(1).save(new CompoundTag()));
        CompoundTag inputs = new CompoundTag();
        for (var entry : panel.targetedBy.entrySet()) {
            var source = FactoryPanelBehaviour.at(panel.getWorld(), entry.getValue());
            if (source == null) return new CompoundTag();
            CompoundTag input = entry.getValue().write();
            input.putUUID("network", source.network);
            input.put("item", source.getFilter().copyWithCount(1).save(new CompoundTag()));
            inputs.put(entry.getKey().pos().asLong() + "/" + entry.getKey().slot().name(), input);
        }
        config.put("inputs", inputs);
        ListTag arrangement = new ListTag();
        panel.activeCraftingArrangement.forEach(stack -> arrangement.add(stack.save(new CompoundTag())));
        config.put("arrangement", arrangement);
        return config;
    }

    public static void configured(FactoryPanelBehaviour panel, ServerPlayer actor) {
        if (!TravelProof.eligible(actor) || panel.getWorld() != actor.level()) return;
        CompoundTag config = configuration(panel);
        if (config.isEmpty()) return;
        CompoundTag owner = new CompoundTag();
        owner.putUUID("actor", actor.getUUID()); owner.putUUID("identity", UUID.randomUUID()); owner.put("configuration", config);
        panel.blockEntity.getPersistentData().put(key(panel), owner); panel.blockEntity.setChanged();
    }

    public static void cycle(FactoryPanelBehaviour panel, Runnable nativeCall) {
        CompoundTag owner = panel.blockEntity.getPersistentData().getCompound(key(panel));
        if (!(panel.getWorld() instanceof ServerLevel level) || ((FactoryPanelBlockEntity)panel.blockEntity).restocker
                || !panel.active || panel.satisfied || panel.promisedSatisfied || panel.waitingForNetwork || panel.redstonePowered
                || panel.targetedBy.isEmpty() || panel.recipeAddress.isBlank() || panel.getFilter().isEmpty() || panel.recipeOutput < 1
                || !owner.hasUUID("actor") || !owner.hasUUID("identity") || !configuration(panel).equals(owner.getCompound("configuration"))) {
            nativeCall.run(); return;
        }
        ServerPlayer actor = level.getServer().getPlayerList().getPlayer(owner.getUUID("actor"));
        if (!TravelProof.eligible(actor)) { nativeCall.run(); return; }
        Cycle previous = CURRENT.get(), run = new Cycle(panel, actor, owner.getCompound("configuration").copy());
        CURRENT.set(run);
        try {
            nativeCall.run();
            if (!run.valid || run.groups.isEmpty() || run.performed.size() != run.groups.size() || run.parcels.isEmpty()
                    || !configuration(panel).equals(run.configuration)) return;
            CompoundTag origin = origin(run);
            Map<String, Long> actual = new HashMap<>();
            for (ItemStack box : run.parcels) {
                CompoundTag proof = CreateParcelProofs.proof(box);
                if (proof == null || !proof.getCompound("gauge").equals(origin)) return;
                add(actual, PackageItem.getContents(box));
            }
            if (!actual.equals(expected(origin))) return;
            origin.putBoolean("fulfilled", true);
            for (ItemStack box : run.parcels) CreateParcelProofs.gauge(box, origin);
        } finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }

    public static void discovered(PackageOrderWithCrafts order, Multimap<PackagerBlockEntity, PackagingRequest> requests) {
        Cycle run = CURRENT.get();
        if (run == null) return;
        if (run.groups.size() >= 32 || requests.isEmpty() || requests.size() > 256 || order.stacks().size() > 128) { run.valid = false; return; }
        run.groups.put(requests, new Group(order, requests));
    }

    public static void perform(Multimap<PackagerBlockEntity, PackagingRequest> requests) {
        Cycle run = CURRENT.get();
        Group group = run == null ? null : run.groups.get(requests);
        if (group == null || !run.valid || run.performed.stream().anyMatch(value -> value == requests)) { LogisticsManager.performPackageRequests(requests); return; }
        run.performed.add(requests);
        CreateOrderProofs.requestPrepared(run.actor, group.order, origin(run), List.copyOf(requests.values()), () -> {
            LogisticsManager.performPackageRequests(requests); return true;
        });
    }

    public static boolean active() { return CURRENT.get() != null; }
    public static void parcel(ItemStack box) {
        Cycle run = CURRENT.get();
        if (run != null && PackageItem.isPackage(box) && box.getCount() == 1 && run.parcels.stream().noneMatch(value -> value == box)) {
            if (run.parcels.size() >= 256) run.valid = false;
            else run.parcels.add(box);
        }
    }

    private static CompoundTag origin(Cycle run) {
        CompoundTag origin = new CompoundTag();
        origin.putBoolean("factory_recipe", true); origin.putUUID("actor", run.actor.getUUID()); origin.putUUID("cycle", run.identity);
        origin.putUUID("configuration_identity", run.panel.blockEntity.getPersistentData().getCompound(key(run.panel)).getUUID("identity"));
        origin.putLong("board", run.panel.getPos().asLong()); origin.putString("slot", run.panel.slot.name());
        origin.putString("dimension", run.actor.level().dimension().location().toString()); origin.putUUID("network", run.panel.network);
        origin.put("configuration", run.configuration.copy()); origin.putInt("batch", run.panel.recipeOutput);
        origin.put("product", run.panel.getFilter().copyWithCount(1).save(new CompoundTag()));
        CompoundTag expected = new CompoundTag();
        ListTag orders = new ListTag();
        for (Group group : run.groups.values()) {
            for (var item : group.order.stacks()) {
                if (item.count > 0 && !item.stack.isEmpty()) {
                    String id = CreateCargoTransfers.fingerprint(item.stack);
                    expected.putLong(id, expected.getLong(id) + item.count);
                }
            }
            for (int id : group.requests.values().stream().mapToInt(PackagingRequest::orderId).distinct().sorted().toArray()) orders.add(IntTag.valueOf(id));
        }
        origin.put("ingredients", expected); origin.put("orders", orders);
        return origin;
    }

    public static Map<String, Long> expected(CompoundTag origin) {
        Map<String, Long> result = new HashMap<>(); CompoundTag items = origin.getCompound("ingredients");
        if (items.size() > 128) return result;
        for (String id : items.getAllKeys()) if (items.getLong(id) > 0) result.put(id, items.getLong(id));
        return result;
    }
    public static void add(Map<String, Long> counts, IItemHandler inventory) {
        if (inventory.getSlots() > 256) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack item = inventory.getStackInSlot(slot);
            if (!item.isEmpty()) counts.merge(CreateCargoTransfers.fingerprint(item), (long)item.getCount(), Long::sum);
        }
    }
    public static Map<String, Long> counts(IItemHandler inventory) {
        Map<String, Long> result = new HashMap<>(); if (inventory != null && inventory.getSlots() <= 256) add(result, inventory); return result;
    }
    public static ListTag snapshot(IItemHandler inventory) {
        ListTag result = new ListTag();
        if (inventory.getSlots() > 256) return result;
        for (int slot = 0; slot < inventory.getSlots(); slot++) result.add(inventory.getStackInSlot(slot).save(new CompoundTag()));
        return result;
    }
    public static FactoryPanelBehaviour panel(ServerLevel level, CompoundTag origin) {
        if (!origin.getBoolean("factory_recipe") || !origin.getBoolean("fulfilled") || !origin.hasUUID("cycle") || !origin.hasUUID("actor")
                || !origin.getString("dimension").equals(level.dimension().location().toString())) return null;
        BlockPos pos = BlockPos.of(origin.getLong("board"));
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null
                || !(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity board)) return null;
        var panel = board.panels.values().stream().filter(value -> value.slot.name().equals(origin.getString("slot"))).findFirst().orElse(null);
        if (panel == null || !panel.network.equals(origin.getUUID("network")) || !configuration(panel).equals(origin.getCompound("configuration"))) return null;
        CompoundTag owner = panel.blockEntity.getPersistentData().getCompound(key(panel));
        return owner.hasUUID("actor") && owner.getUUID("actor").equals(origin.getUUID("actor"))
                && owner.hasUUID("identity") && origin.hasUUID("configuration_identity")
                && owner.getUUID("identity").equals(origin.getUUID("configuration_identity")) ? panel : null;
    }
}
