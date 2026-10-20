package com.siirio.jemcompat.feature.backpack;

/** Native menu extension contract; server and client derive layout from the same native augment snapshot. */
public interface BackpackWorkbench {
    int INSERT_HEIGHT = 80;
    int CRAFT_WIDTH = 112;
    int PANEL_GAP = 4;
    // Backpacked3.0.9: three20px bays, two1px dividers and four2px layout gaps.
    int NATIVE_AUGMENT_PANEL_HEIGHT = 70;
    // Five native10px actions with four2px gaps; outer native label padding is5px.
    int NATIVE_ACTION_PANEL_HEIGHT = 58;
    int GRID_X = 12;
    int GRID_Y = 12;
    int RESULT_X = 85;
    int RESULT_Y = 30;
    int FURNACE_HEIGHT = 80;
    int FURNACE_INPUT_X = 22;
    int FURNACE_FUEL_X = 22;
    int FURNACE_OUTPUT_X = 82;
    boolean jemcompat$hasWorkbench();
    boolean jemcompat$hasFurnace();
    boolean jemcompat$installedWorkbench();
    boolean jemcompat$installedFurnace();
    int jemcompat$furnaceData(int index);
    int jemcompat$nativeWidth();
    default int jemcompat$insertHeight() {
        return jemcompat$hasWorkbench() || jemcompat$hasFurnace() ? INSERT_HEIGHT : 0;
    }
    default int jemcompat$insertWidth() {
        return jemcompat$hasWorkbench() && jemcompat$hasFurnace() ? CRAFT_WIDTH * 2 + PANEL_GAP
                : jemcompat$insertHeight() > 0 ? CRAFT_WIDTH : 0;
    }
    default int jemcompat$workbenchLeft(int nativeWidth) {
        return (nativeWidth - jemcompat$insertWidth()) / 2;
    }
    default int jemcompat$furnaceLeft(int nativeWidth) {
        return jemcompat$workbenchLeft(nativeWidth) + (jemcompat$hasWorkbench() ? CRAFT_WIDTH + PANEL_GAP : 0);
    }
    default int jemcompat$inventoryShift(int rows) {
        return jemcompat$insertHeight() == 0 ? 0
                : jemcompat$insertHeight() + jemcompat$insertTop(rows) - (44 + rows * 18);
    }
    default int jemcompat$insertTop(int rows) {
        int nativePlayerTop = 44 + rows * 18;
        if (jemcompat$insertWidth() <= jemcompat$nativeWidth()) return nativePlayerTop;
        int bayBottom = 16 + (25 + rows * 18 - NATIVE_AUGMENT_PANEL_HEIGHT) / 2
                + NATIVE_AUGMENT_PANEL_HEIGHT + 5;
        int actionsPaddedHeight = NATIVE_ACTION_PANEL_HEIGHT + 10;
        int actionsBottom = 16 + (25 + rows * 18 - actionsPaddedHeight) / 2 + actionsPaddedHeight;
        return Math.max(nativePlayerTop, Math.max(bayBottom, actionsBottom) + PANEL_GAP);
    }
}
