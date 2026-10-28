package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Proof of a real native hopper commit after a paired train interface extraction. */
public final class CreateCargoTransfers {
    private CreateCargoTransfers() {}

    public static String fingerprint(ItemStack input) {
        ItemStack item = input.copy();
        item.setCount(1);
        try {
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(bytes)) {
                CompoundTag saved = item.save(new CompoundTag());
                CompoundTag nativeItem = new CompoundTag();
                // Root-level quest sidecars are observations, not native item identity.
                for (String key : List.of("id", "Count", "tag", "ForgeCaps"))
                    if (saved.contains(key)) nativeItem.put(key, saved.get(key).copy());
                NbtIo.write(nativeItem, output);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Cannot encode native carried item identity", impossible);
        }
    }

    public static Attempt begin(Level world, Hopper inventory) {
        if (!(world instanceof ServerLevel level) || !(inventory instanceof HopperBlockEntity target)
                || level.getBlockEntity(target.getBlockPos()) != target) return null;
        var source = level.getBlockEntity(target.getBlockPos().above());
        if (!(source instanceof PortableItemInterfaceBlockEntity portable) || !portable.canTransfer()
                || !(source instanceof CreateCargoConnection connection)) return null;
        var entity = connection.jem$connectedCarriage();
        if (entity == null || entity.getCarriage() == null) return null;
        Carriage carriage = entity.getCarriage();
        if (entity.getContraption() == null || entity.getContraption().getStorage() != carriage.storage) return null;
        if (!(carriage.train instanceof CreateTrainRun proof) || !proof.jem$cargoReady(carriage)) return null;
        List<ItemStack> before = new ArrayList<>(target.getContainerSize());
        for (int slot = 0; slot < target.getContainerSize(); slot++) before.add(target.getItem(slot).copy());
        return new Attempt(level, source, target, carriage, proof, carriage.storage.getVersion(), List.copyOf(before));
    }

    public static void committed(Attempt attempt, Boolean success) {
        if (attempt == null || !Boolean.TRUE.equals(success)
                || attempt.level.getBlockEntity(attempt.target.getBlockPos()) != attempt.target
                || attempt.level.getBlockEntity(attempt.source.getBlockPos()) != attempt.source
                || attempt.before.size() != attempt.target.getContainerSize()) return;
        ItemStack inserted = ItemStack.EMPTY;
        for (int slot = 0; slot < attempt.before.size(); slot++) {
            ItemStack before = attempt.before.get(slot);
            ItemStack after = attempt.target.getItem(slot);
            if (before.isEmpty() && after.getCount() == 1
                    || ItemStack.isSameItemSameTags(before, after) && after.getCount() == before.getCount() + 1) {
                if (!inserted.isEmpty()) return;
                inserted = after.copy();
                inserted.setCount(1);
            } else if (!ItemStack.matches(before, after)) return;
        }
        if (!inserted.isEmpty()) attempt.proof.jem$cargoDelivered(attempt.carriage, inserted, attempt.version);
    }

    public record Attempt(ServerLevel level, net.minecraft.world.level.block.entity.BlockEntity source,
                          HopperBlockEntity target, Carriage carriage, CreateTrainRun proof,
                          int version, List<ItemStack> before) {}
}
