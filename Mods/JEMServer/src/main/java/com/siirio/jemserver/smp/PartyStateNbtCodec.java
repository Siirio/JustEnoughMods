package com.siirio.jemserver.smp;

import java.util.*;
import net.minecraft.nbt.*;

/** Save compatibility boundary for PartyState. Unknown and untouched tags retain their original representation. */
final class PartyStateNbtCodec {
    static final class Source {
        private final CompoundTag original;
        Source() { original = new CompoundTag(); }
        Source(CompoundTag original) { this.original = original.copy(); }
        CompoundTag copy() { return original.copy(); }
    }
    static PartyState decode(CompoundTag tag) { return PartyState.Codec.decode(tag); }
    static CompoundTag encode(PartyState value) { return PartyState.Codec.encode(value); }
}
