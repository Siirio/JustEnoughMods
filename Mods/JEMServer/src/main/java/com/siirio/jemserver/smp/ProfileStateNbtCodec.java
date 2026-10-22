package com.siirio.jemserver.smp;

import java.util.*;
import net.minecraft.nbt.*;

/** Save compatibility boundary for ProfileState. Unknown and untouched tags retain their original representation. */
final class ProfileStateNbtCodec {
    static final class Source {
        private final CompoundTag original;
        Source() { original = new CompoundTag(); }
        Source(CompoundTag original) { this.original = original.copy(); }
        CompoundTag copy() { return original.copy(); }
    }
    static ProfileState decode(CompoundTag tag) { return ProfileState.Codec.decode(tag); }
    static CompoundTag encode(ProfileState value) { return ProfileState.Codec.encode(value); }
}
