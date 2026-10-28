package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Ephemeral observation of one native helper run and its actual consumed inputs/queued output. */
public final class CreateRepackProof {
    private final List<ItemStack> considered = new ArrayList<>();
    private final List<ItemStack> consumed = new ArrayList<>();
    private List<BigItemStack> output = List.of();
    private int order = -1;
    public void considered(ItemStack box) { considered.add(box.copy()); }
    public void consumed(ItemStack box) { consumed.add(box.copy()); }
    public void result(int order, List<BigItemStack> output) { this.order = order; this.output = output; }
    private static Map<String, Long> contents(List<ItemStack> boxes) {
        Map<String, Long> result = new HashMap<>();
        for (ItemStack box : boxes) {
            var contents = PackageItem.getContents(box);
            for (int slot = 0; slot < contents.getSlots(); slot++) {
                ItemStack item = contents.getStackInSlot(slot);
                if (!item.isEmpty()) result.merge(CreateCargoTransfers.fingerprint(item), (long)item.getCount(), Long::sum);
            }
        }
        return result;
    }
    public void committed(PackagerBlockEntity owner) {
        if (!(owner.getLevel() instanceof ServerLevel level) || order == -1 || output.isEmpty()) return;
        List<ItemStack> expected = considered.stream().filter(box -> PackageItem.getOrderId(box) == order).toList();
        if (expected.size() < 2 || consumed.size() != expected.size()) return;
        UUID actor = null, request = null;
        CompoundTag gauge = null;
        Set<String> coordinates = new HashSet<>();
        Set<UUID> identities = new HashSet<>();
        Set<UUID> consumedIdentities = new HashSet<>();
        Map<String, Integer> expectedParcels = new HashMap<>(), actualParcels = new HashMap<>();
        for (ItemStack box : expected) {
            CompoundTag proof = CreateParcelProofs.proof(box);
            if (proof == null || !proof.hasUUID("parcel") || !PackageItem.hasFragmentData(box)) return;
            if (actor == null) {
                actor = proof.getUUID("actor"); request = proof.getUUID("request");
                gauge = proof.contains("gauge", 10) ? proof.getCompound("gauge") : null;
            }
            if (!actor.equals(proof.getUUID("actor")) || !request.equals(proof.getUUID("request"))
                    || !identities.add(proof.getUUID("parcel"))
                    || !coordinates.add(PackageItem.getLinkIndex(box) + "/" + PackageItem.getIndex(box))) return;
            if (gauge != null && !gauge.equals(proof.getCompound("gauge")) || gauge == null && proof.contains("gauge", 10)) return;
            expectedParcels.merge(CreateCargoTransfers.fingerprint(box), 1, Integer::sum);
        }
        for (ItemStack box : consumed) {
            CompoundTag proof = CreateParcelProofs.proof(box);
            if (proof == null || box.getCount() != 1 || PackageItem.getOrderId(box) != order || !proof.hasUUID("parcel")
                    || !actor.equals(proof.getUUID("actor")) || !request.equals(proof.getUUID("request"))
                    || !consumedIdentities.add(proof.getUUID("parcel"))) return;
            actualParcels.merge(CreateCargoTransfers.fingerprint(box), 1, Integer::sum);
        }
        if (!identities.equals(consumedIdentities) || !expectedParcels.equals(actualParcels)) return;
        Map<String, Long> payload = contents(expected);
        if (payload.isEmpty()) return;
        Map<String, Long> outgoing = new HashMap<>();
        for (BigItemStack parcel : output) {
            if (parcel.count <= 0 || parcel.stack.getCount() != 1 || !PackageItem.isPackage(parcel.stack) || !CreateParcelProofs.nonempty(parcel.stack)
                    || owner.queuedExitingPackages.stream().noneMatch(value -> value == parcel)) return;
            contents(List.of(parcel.stack)).forEach((item, count) -> outgoing.merge(item, count * parcel.count, Long::sum));
        }
        if (!payload.equals(outgoing)) return;
        for (BigItemStack parcel : output) {
            CreateParcelProofs.generated(parcel.stack, actor, request);
            if (gauge != null) CreateParcelProofs.gauge(parcel.stack, gauge);
        }
        CreateRunProofs.emit(level, actor, "create_split_order_repacked");
    }
}
