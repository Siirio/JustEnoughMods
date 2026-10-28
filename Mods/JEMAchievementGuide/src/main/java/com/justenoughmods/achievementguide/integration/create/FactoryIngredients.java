package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Real parcel unpacking assigns proof only to its committed native ingredient jobs. */
public final class FactoryIngredients {
    private static final String INPUT = "jemFactoryInputV4";
    private static final ThreadLocal<Unpack> CURRENT = new ThreadLocal<>();
    private static final class Unpack {
        final CompoundTag origin;
        final Map<String, Long> payload;
        final Map<String, Long> inserted = new HashMap<>();
        final List<FactoryGridReceipt> groups = new ArrayList<>();
        final List<MechanicalCrafterBlockEntity> crafters = new ArrayList<>();
        boolean valid = true;
        Unpack(CompoundTag origin, Map<String, Long> payload) { this.origin = origin; this.payload = payload; }
    }
    private FactoryIngredients() {}

    public static boolean unpack(PackagerBlockEntity receiver, ItemStack box, boolean simulate, BlockPos target,
                                  Supplier<Boolean> nativeCall) {
        CompoundTag parcel = simulate ? null : CreateParcelProofs.proof(box);
        if (parcel == null || !(receiver.getLevel() instanceof ServerLevel level)) return nativeCall.get();
        CompoundTag origin = parcel.getCompound("gauge");
        if (FactoryRecipeProof.panel(level, origin) == null || !parcel.getUUID("actor").equals(origin.getUUID("actor"))
                || origin.getList("orders", 3).stream().noneMatch(tag -> ((net.minecraft.nbt.IntTag)tag).getAsInt() == PackageItem.getOrderId(box))) return nativeCall.get();
        Map<String, Long> payload = FactoryRecipeProof.counts(PackageItem.getContents(box));
        if (payload.isEmpty() || payload.size() > 128 || level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) == null) return nativeCall.get();
        BlockEntity targetOwner = level.getBlockEntity(target);
        BasinBlockEntity basin = targetOwner instanceof BasinBlockEntity value ? value : null;
        CompoundTag previousInput = basin == null ? null : basin.getPersistentData().getCompound(INPUT).copy();
        var before = basin == null ? Map.<String, Long>of() : FactoryRecipeProof.counts(basin.inputInventory);
        boolean basinOwned = basin != null && (before.isEmpty() || previousInput.getCompound("origin").equals(origin)
                && FactoryRecipeProof.snapshot(basin.inputInventory).equals(previousInput.getList("input", 10)));
        Unpack previous = CURRENT.get(), run = new Unpack(origin.copy(), payload);
        CURRENT.set(run);
        boolean success = false;
        try { success = nativeCall.get(); }
        finally {
            finishCrafters(run, success && run.valid && run.inserted.equals(payload));
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
        if (basin != null) {
            var after = FactoryRecipeProof.counts(basin.inputInventory);
            Map<String, Long> expected = new HashMap<>(before); payload.forEach((id, count) -> expected.merge(id, count, Long::sum));
            if (success && basinOwned && after.equals(expected)) {
                CompoundTag proof = new CompoundTag(); proof.put("origin", origin.copy());
                proof.put("input", FactoryRecipeProof.snapshot(basin.inputInventory));
                basin.getPersistentData().put(INPUT, proof);
            } else basin.getPersistentData().remove(INPUT);
            basin.setChanged();
        }
        return success;
    }

    private static void finishCrafters(Unpack run, boolean committed) {
        for (FactoryGridReceipt group : run.groups) {
            CompoundTag proof = group.jem$factoryReceipt();
            if (proof.getCompound("origin").equals(run.origin)) {
                if (committed) proof.putBoolean("confirmed", true);
                group.jem$factoryReceipt(committed ? proof : new CompoundTag());
            }
        }
        for (var crafter : run.crafters) {
            CompoundTag proof = crafter.getPersistentData().getCompound(INPUT);
            if (!proof.getCompound("origin").equals(run.origin)) continue;
            if (committed && crafter.getInventory().getStackInSlot(0).save(new CompoundTag()).equals(proof.getCompound("input"))) {
                proof.putBoolean("confirmed", true);
            } else crafter.getPersistentData().remove(INPUT);
            crafter.setChanged();
        }
    }

    public static void inserted(MechanicalCrafterBlockEntity crafter) {
        if (crafter.getPersistentData().contains(INPUT)) { crafter.getPersistentData().remove(INPUT); crafter.setChanged(); }
        Unpack run = CURRENT.get();
        ItemStack item = crafter.getInventory().getStackInSlot(0);
        if (run == null || item.isEmpty()) return;
        if (run.crafters.size() >= 1024 || run.groups.size() >= 1024) { run.valid = false; return; }
        String id = CreateCargoTransfers.fingerprint(item);
        if (!run.payload.containsKey(id)) return;
        if (run.crafters.stream().noneMatch(value -> value == crafter)) run.crafters.add(crafter);
        run.inserted.merge(id, (long)item.getCount(), Long::sum);
        CompoundTag proof = new CompoundTag(); proof.put("origin", run.origin.copy()); proof.put("input", item.save(new CompoundTag()));
        proof.putBoolean("confirmed", false);
        crafter.getPersistentData().put(INPUT, proof); crafter.setChanged();
    }
    public static CompoundTag beginning(MechanicalCrafterBlockEntity crafter) {
        CompoundTag proof = crafter.getPersistentData().getCompound(INPUT).copy();
        if (crafter.getPersistentData().contains(INPUT)) { crafter.getPersistentData().remove(INPUT); crafter.setChanged(); }
        Unpack run = CURRENT.get();
        if (!proof.getBoolean("confirmed") && (run == null || !proof.getCompound("origin").equals(run.origin))) return new CompoundTag();
        return crafter.getInventory().getStackInSlot(0).save(new CompoundTag()).equals(proof.getCompound("input")) ? proof : new CompoundTag();
    }
    public static void began(FactoryGridReceipt group, CompoundTag input) {
        if (input == null || !input.contains("origin", 10)) return;
        CompoundTag proof = new CompoundTag(); proof.put("origin", input.getCompound("origin").copy()); proof.putString("phase", "ingredients");
        proof.putBoolean("confirmed", CURRENT.get() == null);
        group.jem$factoryReceipt(proof);
        if (CURRENT.get() != null) CURRENT.get().groups.add(group);
    }
    public static CompoundTag basin(BasinBlockEntity basin) {
        CompoundTag proof = basin.getPersistentData().getCompound(INPUT);
        return FactoryRecipeProof.snapshot(basin.inputInventory).equals(proof.getList("input", 10))
                && FactoryRecipeProof.counts(basin.inputInventory).equals(FactoryRecipeProof.expected(proof.getCompound("origin")))
                ? proof.getCompound("origin").copy() : new CompoundTag();
    }
    public static void consumed(BasinBlockEntity basin) {
        if (basin.getPersistentData().contains(INPUT)) { basin.getPersistentData().remove(INPUT); basin.setChanged(); }
    }
}
