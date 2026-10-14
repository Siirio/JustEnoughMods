package com.siirio.jemserver.client.xaero;

import net.minecraftforge.common.ForgeConfigSpec;

/** Local presentation preferences; never stored in Xaero or sent to the server. */
public final class MapLayerPreferences {
    public enum Layer { PROJECTS, EVENTS }
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue PROJECTS = BUILDER.define("projects", false);
    private static final ForgeConfigSpec.BooleanValue EVENTS = BUILDER.define("events", false);
    public static final ForgeConfigSpec SPEC = BUILDER.build();
    private MapLayerPreferences() {}

    public static boolean visible(Layer layer) { return value(layer).get(); }
    public static void setVisible(Layer layer, boolean visible) {
        var preference = value(layer);
        preference.set(visible);
        preference.save();
        if (layer == Layer.PROJECTS && !visible) ProjectRegionClient.cancel();
    }
    private static ForgeConfigSpec.BooleanValue value(Layer layer) {
        return switch (layer) {
            case PROJECTS -> PROJECTS;
            case EVENTS -> EVENTS;
        };
    }
}
