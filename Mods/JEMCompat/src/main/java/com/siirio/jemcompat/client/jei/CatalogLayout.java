package com.siirio.jemcompat.client.jei;

record CatalogLayout(int gridX, int railX, int subRailX, int top, int contentTop, int columns, int rows,
                      int toggleX, int searchX, int searchWidth) {
    int capacity() {
        return columns * rows;
    }
}
