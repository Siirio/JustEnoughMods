package com.siirio.jemprofessions.progress;

import java.util.Locale;
import java.util.Optional;

public enum ProfessionId {
    ARCHITECT,
    FARMER,
    FISHER,
    MINER,
    EXPLORER,
    ENGINEER,
    WARRIOR,
    MERCHANT,
    MAYOR,
    MAGE;

    public static Optional<ProfessionId> parse(String value) {
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
