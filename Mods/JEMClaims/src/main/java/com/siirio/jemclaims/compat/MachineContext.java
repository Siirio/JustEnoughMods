package com.siirio.jemclaims.compat;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;

public final class MachineContext {
    private static final ThreadLocal<ArrayDeque<BlockPos>> SOURCES = new ThreadLocal<>();
    private MachineContext() {}
    public static BlockPos source() {
        ArrayDeque<BlockPos> sources = SOURCES.get();
        return sources == null ? null : sources.peek();
    }
    public static void push(BlockPos source) {
        ArrayDeque<BlockPos> sources = SOURCES.get();
        if (sources == null) {
            sources = new ArrayDeque<>();
            SOURCES.set(sources);
        }
        sources.push(source);
    }
    public static void pop() {
        ArrayDeque<BlockPos> sources = SOURCES.get();
        sources.pop();
        if (sources.isEmpty()) SOURCES.remove();
    }
    public static void run(BlockPos source, Runnable action) {
        push(source);
        try { action.run(); }
        finally { pop(); }
    }
}
