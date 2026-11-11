package dev.taste.engine.effects.glass;

/** Exact world face identity keeps drops on the pane while the camera moves. */
public record GlassPatchKey(String dimension, int blockX, int blockY, int blockZ, int face) { }
