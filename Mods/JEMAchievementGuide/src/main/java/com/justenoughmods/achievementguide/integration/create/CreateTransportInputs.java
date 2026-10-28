package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Transient call scopes, with durable proof stored only on the native transported job. */
public final class CreateTransportInputs {
    private record Input(UUID actor, String fingerprint) {}
    private static final ThreadLocal<Input> INPUT = new ThreadLocal<>();
    private static final ThreadLocal<TransportedItemStack> PROCESSING = new ThreadLocal<>();
    private CreateTransportInputs() {}

    public static ItemStack insert(ItemEntity entity, Supplier<ItemStack> nativeCall) {
        Input previous = INPUT.get();
        UUID actor = CreateRunProofs.actor(entity);
        if (actor == null) return nativeCall.get();
        INPUT.set(new Input(actor, CreateCargoTransfers.fingerprint(entity.getItem())));
        try { return nativeCall.get(); }
        finally { if (previous == null) INPUT.remove(); else INPUT.set(previous); }
    }
    public static CompoundTag initial(ItemStack item) {
        CompoundTag proof = new CompoundTag();
        Input input = INPUT.get();
        if (input != null && !item.isEmpty() && input.fingerprint().equals(CreateCargoTransfers.fingerprint(item))) {
            proof.putUUID("actor", input.actor());
            proof.put("stack", item.save(new CompoundTag()));
        }
        return proof;
    }
    public static <T> T processing(TransportedItemStack input, Supplier<T> nativeCall) {
        TransportedItemStack previous = PROCESSING.get();
        ((CreateTransportProof)(Object)input).jem$clearAdvance();
        PROCESSING.set(input);
        try { return nativeCall.get(); }
        finally { if (previous == null) PROCESSING.remove(); else PROCESSING.set(previous); }
    }
    public static void advanced(ItemStack input, ItemStack output, String recipe, int step, boolean completed) {
        TransportedItemStack actual = PROCESSING.get();
        if (actual != null && !input.isEmpty() && !output.isEmpty()
                && CreateCargoTransfers.fingerprint(input).equals(CreateCargoTransfers.fingerprint(actual.stack))) {
            ((CreateTransportProof)(Object)actual).jem$nativeAdvance(output, recipe, step, completed);
        }
    }
    public static void committed(ServerLevel level, TransportedItemStack output) {
        CreateTransportProof nativeProof = (CreateTransportProof)(Object)output;
        CompoundTag completed = nativeProof.jem$transportProof();
        if (completed.getBoolean("film")) {
            UUID actor = nativeProof.jem$supplier();
            completed.remove("film");
            nativeProof.jem$transportProof(completed);
            CreateRunProofs.emit(level, actor, "v3_eng_photo_develop");
        }
        if (completed.getBoolean("spout")) {
            UUID actor = nativeProof.jem$supplier();
            completed.remove("spout");
            nativeProof.jem$transportProof(completed);
            CreateRunProofs.emit(level, actor, "create_spout_fill");
        }
        if (nativeProof.jem$precisionCompleted()) {
            UUID actor = nativeProof.jem$supplier();
            CompoundTag proof = nativeProof.jem$transportProof();
            proof.remove("precision");
            nativeProof.jem$transportProof(proof);
            CreateRunProofs.emit(level, actor, "create_precision_assembly");
        }
    }
}
