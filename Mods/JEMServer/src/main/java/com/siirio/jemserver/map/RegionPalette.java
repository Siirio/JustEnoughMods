package com.siirio.jemserver.map;

import java.util.UUID;

public final class RegionPalette {
    private static final int[] COLORS = {0xB87333, 0x4F8A5B, 0xC6923E, 0x8E5AA7, 0x4D83A6, 0xA65353};
    private RegionPalette() { }
    public static int forProject(UUID id) { return COLORS[Math.floorMod(id.hashCode(), COLORS.length)]; }
}
