package com.siirio.jemroles.race;

import java.util.Locale;
import java.util.Optional;

public enum RaceId {
    ANCESTORS,
    VOID_WALKERS,
    BORN_IN_BLOOM,
    INFECTED_BY_SCULK,
    NETHER_HEARTEDS;

    public static Optional<RaceId> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toUpperCase(Locale.ROOT);
        normalized = switch (normalized) {
            case "HUMAN" -> "ANCESTORS";
            case "DWARF" -> "INFECTED_BY_SCULK";
            case "ELF" -> "BORN_IN_BLOOM";
            default -> normalized;
        };
        try {
            return Optional.of(valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public String key() { return name().toLowerCase(Locale.ROOT); }
}
