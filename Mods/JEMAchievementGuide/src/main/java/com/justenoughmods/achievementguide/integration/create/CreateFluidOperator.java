package com.justenoughmods.achievementguide.integration.create;

import java.util.UUID;
import java.util.function.Supplier;

/** One native flow call, never an owner lookup or independent fluid graph. */
public final class CreateFluidOperator {
    private static final ThreadLocal<UUID> ACTOR = new ThreadLocal<>();
    private CreateFluidOperator() {}
    public static UUID actor() { return ACTOR.get(); }
    public static <T> T transfer(UUID actor, Supplier<T> nativeCall) {
        UUID previous = ACTOR.get();
        if (actor == null) ACTOR.remove(); else ACTOR.set(actor);
        try { return nativeCall.get(); }
        finally { if (previous == null) ACTOR.remove(); else ACTOR.set(previous); }
    }
}
