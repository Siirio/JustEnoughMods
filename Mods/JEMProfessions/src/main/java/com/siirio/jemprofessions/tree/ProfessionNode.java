package com.siirio.jemprofessions.tree;

public record ProfessionNode(String id, String branch, int level, boolean toggleable, String dependency) {
    public ProfessionNode {
        if (id == null || branch == null || level < 1 || level > 50) throw new IllegalArgumentException("Invalid profession node");
        dependency = dependency == null ? "" : dependency;
    }
}
