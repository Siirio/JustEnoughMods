package com.siirio.jemprofessions.tree;

public record ProfessionNode(String id, String branch, int rank, String parentId, int level,
                             boolean toggleable, String dependency, Availability availability, String effectKey) {
    public enum Availability { AVAILABLE, GUIDE_ONLY, UNSUPPORTED_NATIVE_API, PLANNED_AND_HIDDEN }
    public ProfessionNode {
        if (id == null || id.isBlank() || branch == null || branch.isBlank() || rank < 1 || level < 1 || level > 50)
            throw new IllegalArgumentException("Invalid profession node");
        parentId = parentId == null ? "" : parentId;
        dependency = dependency == null ? "" : dependency;
        availability = availability == null ? Availability.UNSUPPORTED_NATIVE_API : availability;
        effectKey = effectKey == null || effectKey.isBlank() ? "jem.professions.node." + id + ".effect" : effectKey;
    }
    public boolean available() { return availability == Availability.AVAILABLE; }
}
