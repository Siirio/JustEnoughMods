package com.siirio.jemprofessions.api;

public record ProfessionProgressResult(Status status, int previousLevel, int currentLevel, long totalXp) {
    public enum Status { ACCEPTED, DUPLICATE, UNAUTHENTICATED }
}
