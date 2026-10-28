package com.justenoughmods.achievementguide.integration.create;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

/** One synchronous native request call, never a persistent logistics/progress owner. */
public final class CreateOrderProofs {
    private static final ThreadLocal<ArrayDeque<Request>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);
    private CreateOrderProofs() {}

    public static boolean request(ServerPlayer actor, PackageOrderWithCrafts order, Supplier<Boolean> nativeCall) {
        if (actor == null || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild || order.isEmpty()) {
            return nativeCall.get();
        }
        return request(actor, order, null, null, nativeCall);
    }

    public static boolean request(ServerPlayer actor, PackageOrderWithCrafts order, String completionAction, CompoundTag gauge, Supplier<Boolean> nativeCall) {
        if (actor == null || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild || order.isEmpty()) return nativeCall.get();
        Request proof = new Request(actor, order);
        proof.gauge = gauge;
        var calls = CALLS.get();
        calls.push(proof);
        try {
            boolean accepted = nativeCall.get();
            if (proof.packages > 0) QuestActionTrigger.INSTANCE.emit(actor, "create_package_filled");
            if (accepted && !proof.needed.isEmpty() && proof.needed.entrySet().stream()
                    .allMatch(entry -> proof.shipped.getOrDefault(entry.getKey(), 0L) >= entry.getValue())) {
                QuestActionTrigger.INSTANCE.emit(actor, "create_stock_order_fulfilled");
                if (completionAction != null) QuestActionTrigger.INSTANCE.emit(actor, completionAction);
                if (proof.gauge != null) proof.boxes.forEach(box -> CreateParcelProofs.gauge(box, proof.gauge));
            }
            return accepted;
        } finally {
            calls.pop();
            if (calls.isEmpty()) CALLS.remove();
        }
    }

    public static boolean active() { return !CALLS.get().isEmpty(); }

    /** Already-discovered native Factory Gauge requests, preserving the same ordinary request observer. */
    public static boolean requestPrepared(ServerPlayer actor, PackageOrderWithCrafts order, CompoundTag origin,
                                          Collection<PackagingRequest> requests, Supplier<Boolean> nativeCall) {
        return request(actor, order, null, origin, () -> {
            discovered(order, requests);
            return nativeCall.get();
        });
    }

    public static void discovered(PackageOrderWithCrafts order, Collection<PackagingRequest> requests) {
        Request proof = CALLS.get().peek();
        if (proof == null || proof.order != order) return;
        for (PackagingRequest request : requests) proof.orderIds.add(request.orderId());
    }

    public static void packaged(ItemStack box) {
        Request proof = CALLS.get().peek();
        if (proof == null || !PackageItem.isPackage(box) || !PackageItem.hasFragmentData(box)
                || !proof.orderIds.contains(PackageItem.getOrderId(box))) return;
        String parcel = PackageItem.getOrderId(box) + "/" + PackageItem.getLinkIndex(box) + "/" + PackageItem.getIndex(box);
        if (!proof.parcels.add(parcel)) return;
        var contents = PackageItem.getContents(box);
        boolean nonempty = false;
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack item = contents.getStackInSlot(slot);
            if (item.isEmpty()) continue;
            nonempty = true;
            proof.shipped.merge(CreateCargoTransfers.fingerprint(item), (long)item.getCount(), Long::sum);
        }
        if (nonempty) {
            CreateParcelProofs.generated(box, proof.actor.getUUID(), proof.identity);
            proof.boxes.add(box);
            proof.packages++;
        }
    }

    private static final class Request {
        final ServerPlayer actor;
        final UUID identity = UUID.randomUUID();
        final PackageOrderWithCrafts order;
        final Map<String, Long> needed = new HashMap<>();
        final Map<String, Long> shipped = new HashMap<>();
        final Set<Integer> orderIds = new HashSet<>();
        final Set<String> parcels = new HashSet<>();
        final java.util.List<ItemStack> boxes = new java.util.ArrayList<>();
        int packages;
        CompoundTag gauge;
        Request(ServerPlayer actor, PackageOrderWithCrafts order) {
            this.actor = actor;
            this.order = order;
            for (var item : order.stacks()) {
                if (!item.stack.isEmpty() && item.count > 0) {
                    needed.merge(CreateCargoTransfers.fingerprint(item.stack), (long)item.count, Long::sum);
                }
            }
        }
    }
}
