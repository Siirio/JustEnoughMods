package com.siirio.jemworldbosstiers.revival;

public final class ArenaDestructionPolicy {
    private ArenaDestructionPolicy() {
    }

    public static boolean canDestroy(boolean insideArena, boolean blockEntity, boolean protectedBlock) {
        return insideArena && !blockEntity && !protectedBlock;
    }
}
