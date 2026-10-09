package com.siirio.jemvillagertalking.speech;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class VillagerReactionCoverageTest {
    private static final int EXPECTED_EVENTS = 417;
    private static final int EXPECTED_LINES = 1083;

    @Test
    void everyCatalogEventHasProductionRegistration() {
        int catalogEvents = VillagerSpeechCatalog.eventCount();
        int implementedEvents = VillagerReactionRegistry.registeredCount();
        int reactionLines = VillagerSpeechCatalog.lineCount();
        System.out.printf(
                "implemented_events=%d catalog_events=%d reaction_lines=%d%n",
                implementedEvents,
                catalogEvents,
                reactionLines
        );
        assertEquals(EXPECTED_EVENTS, catalogEvents);
        assertEquals(EXPECTED_EVENTS, implementedEvents);
        assertEquals(EXPECTED_LINES, reactionLines);
        assertEquals(VillagerReactionRegistry.catalogEventIds(), VillagerReactionRegistry.registeredEventIds());
    }
}
