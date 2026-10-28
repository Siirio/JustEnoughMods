package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.logistics.box.PackageItem;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

/** Causal proof on the actual native parcel, without another order/progress registry. */
public final class CreateParcelProofs {
    public static final String SAVE_KEY = "jemQuestParcelV1";
    private CreateParcelProofs() {}

    public static CompoundTag proof(ItemStack box) {
        CompoundTag proof = ((CreateParcelCarrier)(Object)box).jem$parcelProof();
        if (proof == null || box.getCount() != 1 || !PackageItem.isPackage(box) || !proof.hasUUID("actor")
                || !proof.hasUUID("request") || !box.hasTag()
                || !proof.getString("item").equals(String.valueOf(ForgeRegistries.ITEMS.getKey(box.getItem())))
                || !nativeTagFingerprint(box).equals(proof.getString("native_tag_sha256"))
                || PackageItem.getOrderId(box) != proof.getInt("order")
                || PackageItem.getLinkIndex(box) != proof.getInt("link") || PackageItem.getIndex(box) != proof.getInt("index")) return null;
        return proof;
    }
    public static void generated(ItemStack box, UUID actor, UUID request) {
        if (actor == null || request == null || box.getCount() != 1 || !PackageItem.isPackage(box) || !box.hasTag() || !nonempty(box)) return;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", actor); proof.putUUID("request", request); proof.putUUID("parcel", UUID.randomUUID());
        proof.putString("item", String.valueOf(ForgeRegistries.ITEMS.getKey(box.getItem())));
        proof.putString("native_tag_sha256", nativeTagFingerprint(box));
        proof.putInt("order", PackageItem.getOrderId(box));
        proof.putInt("link", PackageItem.getLinkIndex(box));
        proof.putInt("index", PackageItem.getIndex(box));
        ((CreateParcelCarrier)(Object)box).jem$parcelProof(proof);
    }
    public static void gauge(ItemStack box, CompoundTag gauge) {
        CompoundTag proof = proof(box);
        if (proof == null) return;
        proof.put("gauge", gauge.copy());
        ((CreateParcelCarrier)(Object)box).jem$parcelProof(proof);
    }
    private static String nativeTagFingerprint(ItemStack box) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) { NbtIo.write((CompoundTag)canonical(box.getTag()), output); }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) { throw new IllegalStateException("Cannot fingerprint native parcel tag", exception); }
    }
    private static Tag canonical(Tag value) {
        if (value instanceof CompoundTag compound) {
            CompoundTag sorted = new CompoundTag();
            compound.getAllKeys().stream().sorted().forEach(key -> sorted.put(key, canonical(compound.get(key))));
            return sorted;
        }
        if (value instanceof ListTag list) {
            ListTag copy = new ListTag();
            for (Tag element : list) copy.add(canonical(element));
            return copy;
        }
        return value.copy();
    }
    public static boolean nonempty(ItemStack box) {
        var contents = PackageItem.getContents(box);
        for (int slot = 0; slot < contents.getSlots(); slot++) if (!contents.getStackInSlot(slot).isEmpty()) return true;
        return false;
    }
    public static void exported(ItemStack box, LevelAccessor world, BlockPos sender) {
        CompoundTag proof = proof(box);
        if (proof == null || !(world instanceof ServerLevel level)) return;
        proof.putLong("sender", sender.asLong());
        proof.putString("dimension", level.dimension().location().toString());
        proof.remove("receiver");
        proof.putBoolean("chain", true);
        ((CreateParcelCarrier)(Object)box).jem$parcelProof(proof);
    }
    public static void arriving(ItemStack box, BlockEntity receiver) {
        CompoundTag proof = proof(box);
        if (proof == null || !proof.getBoolean("chain") || !(receiver.getLevel() instanceof ServerLevel level)
                || !proof.getString("dimension").equals(level.dimension().location().toString())
                || proof.getLong("sender") == receiver.getBlockPos().asLong()) return;
        proof.putLong("receiver", receiver.getBlockPos().asLong());
        ((CreateParcelCarrier)(Object)box).jem$parcelProof(proof);
    }
    public static void received(CompoundTag proof, BlockEntity receiver) {
        if (proof == null || !proof.getBoolean("chain") || !proof.contains("receiver")
                || !(receiver.getLevel() instanceof ServerLevel level)
                || !proof.getString("dimension").equals(level.dimension().location().toString())
                || proof.getLong("receiver") != receiver.getBlockPos().asLong() || proof.getLong("sender") == receiver.getBlockPos().asLong()) return;
        CreateRunProofs.emit(level, proof.getUUID("actor"), "create_chain_delivery");
    }
}
