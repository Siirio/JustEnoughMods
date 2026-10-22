package com.siirio.jemserver.smp;

/** Fishing event score; unknown persisted fields remain codec metadata. */
public final class EventFishingScore {
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event score"); }
    void freeze() { frozen = true; }
    public EventFishingScore copy() {
        var result = new EventFishingScore(); result.source = source.copy();
        result.catches = catches; result.catchesChanged = catchesChanged;
        result.points = points; result.pointsChanged = pointsChanged;
        result.fishingXp = fishingXp; result.fishingXpChanged = fishingXpChanged;
        return result;
    }
    private Integer catches;
    boolean catchesChanged;
    public int catches() { return catches == null ? 0 : catches; }
    public void catches(int value) { requireMutable(); catches = value; catchesChanged = true; }
    public boolean hasCatches() { return catches != null; }
    private Integer points;
    boolean pointsChanged;
    public int points() { return points == null ? 0 : points; }
    public void points(int value) { requireMutable(); points = value; pointsChanged = true; }
    public boolean hasPoints() { return points != null; }
    private Integer fishingXp;
    boolean fishingXpChanged;
    public int fishingXp() { return fishingXp == null ? 0 : fishingXp; }
    public void fishingXp(int value) { requireMutable(); fishingXp = value; fishingXpChanged = true; }
    public boolean hasFishingXp() { return fishingXp != null; }
}
