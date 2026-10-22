package com.justenoughmods.achievementguide.client.handbook;

import com.siirio.jemdrill.drill.DrillWearPolicy;

/** Common text tokens read domain constants; this class owns no drill rules. */
final class HandbookText {
    private HandbookText() { }

    static String drillPolicy(String field) {
        return switch (field) {
            case "wear" -> Integer.toString(DrillWearPolicy.MAX_WEAR);
            case "dull" -> Integer.toString(DrillWearPolicy.DULL_WEAR);
            case "repairs" -> Integer.toString(DrillWearPolicy.MAX_REPAIRS);
            default -> throw new IllegalArgumentException("Unknown drill policy field: " + field);
        };
    }
}
