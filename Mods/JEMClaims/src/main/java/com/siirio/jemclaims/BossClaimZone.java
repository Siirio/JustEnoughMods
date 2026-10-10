package com.siirio.jemclaims;

record BossClaimZone(String dimension, int minX, int minZ, int maxX, int maxZ) {
    boolean overlaps(String dimension, int minX, int minZ, int maxX, int maxZ, int margin) {
        return this.dimension.equals(dimension) && (long) this.minX - margin <= maxX && (long) this.maxX + margin >= minX
                && (long) this.minZ - margin <= maxZ && (long) this.maxZ + margin >= minZ;
    }
}
