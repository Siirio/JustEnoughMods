package dev.taste.engine.render.embeddium;

import java.util.ArrayList;
import java.util.List;

/** Independent capture list. It never aliases Embeddium's camera render lists. */
public final class EmbeddiumCaptureRenderer {
    public record Region(int regionX, int regionY, int regionZ, long generation) { }
    private final List<Region> regions = new ArrayList<>();
    public void add(int x, int y, int z, long generation) { regions.add(new Region(x, y, z, generation)); }
    public List<Region> snapshot() { return List.copyOf(regions); }
    public void clear() { regions.clear(); }
}
