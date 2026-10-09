package com.siirio.jemadaptiveculling.client;

public final class GeometryTracker {
    private static long revision;

    private GeometryTracker() {
    }

    public static void changed() {
        revision++;
    }

    public static long revision() {
        return revision;
    }
}
