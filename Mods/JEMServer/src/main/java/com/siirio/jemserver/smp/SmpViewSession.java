package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

final class SmpViewSession {
    SmpQuery query;
    UUID id = UUID.randomUUID();
    CompoundTag sent;
    SmpViewRevision owners;
    long sequence;
    long builtAt = Long.MIN_VALUE;

    SmpViewSession(SmpQuery query) { this.query = query.withoutReset(); }

    void select(SmpQuery next) {
        if (!query.equals(next.withoutReset())) {
            query = next.withoutReset();
            id = UUID.randomUUID();
            sequence = 0;
            sent = null;
        } else if (next.reset()) sent = null;
    }

    SmpUpdate update(CompoundTag next, boolean open, UUID request, String error) {
        boolean delta = sent != null && !open && request == null;
        long base = delta ? sequence : 0;
        var payload = delta ? ViewDelta.between(sent, next) : next;
        sent = next.copy();
        return new SmpUpdate(payload, open, request, error, id, base, ++sequence);
    }
}
