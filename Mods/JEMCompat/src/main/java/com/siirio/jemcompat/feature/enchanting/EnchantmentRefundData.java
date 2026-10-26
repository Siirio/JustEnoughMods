package com.siirio.jemcompat.feature.enchanting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** A paid XP credit may be redeemed once even if its ItemStack NBT was copied. */
public final class EnchantmentRefundData extends SavedData {
    private static final String NAME = "jemcompat_enchantment_refunds";
    private final Set<UUID> redeemed = new HashSet<>();

    public static EnchantmentRefundData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EnchantmentRefundData::load,
                EnchantmentRefundData::new, NAME);
    }

    private static EnchantmentRefundData load(CompoundTag tag) {
        EnchantmentRefundData data = new EnchantmentRefundData();
        ListTag values = tag.getList("Redeemed", 10);
        for (int index = 0; index < values.size(); index++) {
            CompoundTag value = values.getCompound(index);
            if (value.hasUUID("Token")) data.redeemed.add(value.getUUID("Token"));
        }
        return data;
    }

    public boolean canRedeem(List<EnchantmentCostHistory.Credit> credits) {
        return !credits.isEmpty() && credits.stream().noneMatch(credit -> redeemed.contains(credit.token()));
    }

    public void redeem(List<EnchantmentCostHistory.Credit> credits) {
        for (var credit : credits) redeemed.add(credit.token());
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag values = new ListTag();
        for (UUID token : redeemed) {
            CompoundTag value = new CompoundTag();
            value.putUUID("Token", token);
            values.add(value);
        }
        tag.put("Redeemed", values);
        return tag;
    }
}
