package com.siirio.jemserver.smp;

import java.util.*;
import net.minecraft.nbt.*;

/** Save compatibility boundary for PartyMember. Unknown and untouched tags retain their original representation. */
final class PartyMemberNbtCodec {
    static final class Source {
        private final CompoundTag original;
        Source() { original = new CompoundTag(); }
        Source(CompoundTag original) { this.original = original.copy(); }
        CompoundTag copy() { return original.copy(); }
    }
    static PartyMember decode(CompoundTag tag) { return PartyMember.Codec.decode(tag); }
    static CompoundTag encode(PartyMember value) { return PartyMember.Codec.encode(value); }
}
