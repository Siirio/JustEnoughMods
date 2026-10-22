package com.siirio.jemserver.smp;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

/** Lossless immutable native serialization payload; domain code does not inspect its keys. */
public final class NativeNbtPayload {
    private final Tag encoded;
    public NativeNbtPayload(Tag encoded) { this.encoded = encoded.copy(); }
    public Tag copyNativeTag() { return encoded.copy(); }
    /** Same element-type contract as CompoundTag.getList, without exposing stored payloads. */
    public ListTag compoundList() {
        return encoded instanceof ListTag list && (list.isEmpty() || list.getElementType() == Tag.TAG_COMPOUND)
                ? list.copy() : new ListTag();
    }
}
