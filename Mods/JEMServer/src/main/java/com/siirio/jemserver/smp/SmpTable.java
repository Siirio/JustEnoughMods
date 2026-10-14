package com.siirio.jemserver.smp;

import java.util.Locale;

public enum SmpTable {
    PARTIES, SHOPS, EVENTS, PROFILES, PRIZES;

    private static final java.util.Set<String> READ_ONLY_ROUTES =
            java.util.Set.of("home", "settlements", "recruits");

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SmpTable fromKey(String key) {
        return valueOf(key.toUpperCase(Locale.ROOT));
    }

    public static boolean viewable(String key) {
        if (READ_ONLY_ROUTES.contains(key)) return true;
        try {
            fromKey(key);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
