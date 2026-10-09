package com.justenoughmods.achievementguide;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdvancementTextPolicyTest {
    private static final Pattern PLAYER_FACING_TEXT = Pattern.compile(
            "\\\"[^\\\"]*(?:description|guide|subject|landing_text|text)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\""
    );
    private static final List<String> FORBIDDEN_NAMES = List.of(
            "Alex's Mobs",
            "Alex's Caves",
            "Aquamirae",
            "BackToBed",
            "Backpacked",
            "Cataclysm",
            "Create's",
            "Critters n’ Crawlers",
            "Epic Fight",
            "Gifty Present",
            "Immersive Melodies",
            "JEI",
            "L_Ender's Cataclysm",
            "MOA DECOR",
            "Nightfall",
            "Rotten Creatures",
            "Sooty Chimneys",
            "Spyglass Improvements",
            "ZiplineLogicMixin"
    );

    @Test
    void advancementAndGuideDescriptionsDoNotNameMods() throws IOException {
        assertClean("en_us.json");
        assertClean("ru_ru.json");
    }

    @Test
    void patchouliDescriptionsDoNotNameMods() throws IOException, URISyntaxException {
        var resource = AdvancementTextPolicyTest.class.getResource("/assets/jem_advancements/patchouli_books");
        assertNotNull(resource);
        try (var paths = Files.walk(Path.of(resource.toURI()))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                assertCleanText(Files.readString(path, StandardCharsets.UTF_8));
            }
        }
    }

    private static void assertClean(String language) throws IOException {
        String resource = "/assets/jem_advancements/lang/" + language;
        try (InputStream stream = AdvancementTextPolicyTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            assertCleanText(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static void assertCleanText(String text) {
        Matcher matcher = PLAYER_FACING_TEXT.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(1);
            FORBIDDEN_NAMES.forEach(name -> assertFalse(value.contains(name), value));
        }
    }
}
