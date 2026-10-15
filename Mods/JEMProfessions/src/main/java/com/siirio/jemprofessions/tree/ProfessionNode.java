package com.siirio.jemprofessions.tree;

/** One stable node in the JEM presentation of the Pufferfish-owned profession graph. */
public record ProfessionNode(String id, String branch, int rank, String parentId, int level, String icon) {
    public ProfessionNode {
        if (id == null || id.isBlank() || branch == null || branch.isBlank() || rank < 1 || level < 1 || level > 50) {
            throw new IllegalArgumentException("Invalid profession node");
        }
        parentId = parentId == null ? "" : parentId;
        icon = icon == null || icon.isBlank() ? "minecraft:book" : icon;
    }
}
