package com.siirio.jemroles.race;

import java.util.Locale;
import java.util.Optional;

public enum RaceId {
    HUMAN,
    DWARF,
    ELF;

    public static Optional<RaceId> parse(String value) {
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
