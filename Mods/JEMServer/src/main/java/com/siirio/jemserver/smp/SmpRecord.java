package com.siirio.jemserver.smp;

import java.util.UUID;

/** Identity and lifecycle shared by read-only SMP record views. */
public interface SmpRecord {
    UUID id();
    UUID owner();
    int revision();
    long created();
    String state();
    default boolean closed() {
        return java.util.Set.of("CLOSED", "CANCELLED", "COMPLETED", "FAILED", "SKIPPED").contains(state());
    }
}
