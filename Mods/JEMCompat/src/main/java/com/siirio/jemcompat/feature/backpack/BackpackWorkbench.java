package com.siirio.jemcompat.feature.backpack;

/** Native menu extension contract; server and client derive layout from the same native augment snapshot. */
public interface BackpackWorkbench {
    int INSERT_HEIGHT = 80;
    int CRAFT_WIDTH = 112;
    int GRID_X = 12;
    int GRID_Y = 12;
    int RESULT_X = 85;
    int RESULT_Y = 30;
    boolean jemcompat$hasWorkbench();
}
