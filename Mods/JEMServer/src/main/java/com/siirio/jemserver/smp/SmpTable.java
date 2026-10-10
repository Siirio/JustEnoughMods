package com.siirio.jemserver.smp;

import java.util.Locale;

public enum SmpTable {
    PARTIES, SHOPS, EVENTS, PROFILES, PRIZES;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SmpTable fromKey(String key) {
        return valueOf(key.toUpperCase(Locale.ROOT));
    }
}
