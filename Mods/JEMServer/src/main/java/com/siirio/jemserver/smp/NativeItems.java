package com.siirio.jemserver.smp;

import net.minecraft.nbt.ListTag;

/** Opaque immutable native ItemStack payload, interpreted only by inventory/reward adapters. */
public final class NativeItems {
    private final ListTag items;
    private NativeItems(ListTag items) { this.items = items.copy(); }
    public static NativeItems empty() { return new NativeItems(new ListTag()); }
    public static NativeItems fromNbt(ListTag items) { return new NativeItems(items); }
    public ListTag toNbt() { return items.copy(); }
    public boolean isEmpty() { return items.isEmpty(); }
}
