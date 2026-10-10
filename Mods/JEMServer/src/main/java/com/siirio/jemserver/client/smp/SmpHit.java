package com.siirio.jemserver.client.smp;

record SmpHit(int x, int y, int width, int height, Runnable action) {
    boolean contains(double px, double py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
}
