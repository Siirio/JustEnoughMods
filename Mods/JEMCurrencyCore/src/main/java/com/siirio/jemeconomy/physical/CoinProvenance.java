package com.siirio.jemeconomy.physical;

import com.siirio.jemcurrency.CurrencyItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class CoinProvenance {
    private static final String ROOT = "JemPhysicalCoin";
    private static final String LOT = "Lot";
    private static final String OPERATION = "Operation";
    private static final String DENOMINATION = "Denomination";
    private static final String SCHEMA = "Schema";

    private CoinProvenance() {
    }

    public static ItemStack issue(CoinDenomination denomination, int count, UUID lotId, UUID operationId) {
        ItemStack stack = new ItemStack(CurrencyItems.item(denomination), count);
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA, 1);
        tag.putUUID(LOT, lotId);
        tag.putUUID(OPERATION, operationId);
        tag.putString(DENOMINATION, denomination.id());
        stack.getOrCreateTag().put(ROOT, tag);
        return stack;
    }

    public static Optional<Stamp> read(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PhysicalCoinItem item)) return Optional.empty();
        CompoundTag root = stack.getTagElement(ROOT);
        if (root == null || root.getInt(SCHEMA) != 1 || !root.hasUUID(LOT) || !root.hasUUID(OPERATION)) return Optional.empty();
        Optional<CoinDenomination> denomination = CoinDenomination.byId(root.getString(DENOMINATION));
        if (denomination.isEmpty() || denomination.get() != item.denomination()) return Optional.empty();
        return Optional.of(new Stamp(root.getUUID(LOT), root.getUUID(OPERATION), denomination.get()));
    }

    public record Stamp(UUID lotId, UUID operationId, CoinDenomination denomination) {
    }
}
