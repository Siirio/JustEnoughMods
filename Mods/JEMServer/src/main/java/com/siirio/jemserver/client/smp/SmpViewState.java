package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.*;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** The screen's transport state; never reads widgets or changes navigation. */
final class SmpViewState {
    record Reception(CompoundTag view, boolean ignored, boolean resync) {}
    private UUID session;
    private long revision;
    private long lastResync;
    private CompoundTag model = new CompoundTag();
    private SmpAction pending;

    Reception accept(SmpUpdate update, long now) {
        if (update.view() == null || update.view().isEmpty()) return new Reception(null, false, false);
        boolean delta = update.view().getBoolean("__delta");
        if (delta && (!Objects.equals(session, update.session()) || revision != update.baseRevision())) {
            boolean resync = now - lastResync >= SmpProtocol.RESYNC_MILLIS;
            if (resync) lastResync = now;
            return new Reception(null, true, resync);
        }
        if (update.session() != null) {
            if (Objects.equals(session, update.session()) && update.revision() <= revision)
                return new Reception(null, true, false);
            session = update.session();
            revision = update.revision();
        }
        model = delta ? ViewDelta.apply(model, update.view()) : update.view().copy();
        return new Reception(model.copy(), false, false);
    }

    boolean waiting() { return pending != null; }

    void begin(SmpAction action) {
        if (pending != null) throw new IllegalStateException("SMP request already pending");
        pending = Objects.requireNonNull(action);
    }

    SmpAction acknowledge(SmpUpdate update) {
        if (pending == null || !pending.request().equals(update.request())) return null;
        SmpAction accepted = pending;
        pending = null;
        return accepted;
    }
}
