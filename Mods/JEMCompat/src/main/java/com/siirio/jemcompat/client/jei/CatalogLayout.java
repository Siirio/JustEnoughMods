package com.siirio.jemcompat.client.jei;

record CatalogLayout(int gridX, int railX, int subRailX, int top, int contentTop, int columns, int rows,
                      int toggleX, int searchX, int searchY, int searchWidth, int navigationY, int railSlots) {
    int capacity() {
        return Math.max(1, columns * rows);
    }
}
