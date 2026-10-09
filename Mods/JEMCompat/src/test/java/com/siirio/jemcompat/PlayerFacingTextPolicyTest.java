package com.siirio.jemcompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerFacingTextPolicyTest {
    private static final Pattern DESCRIPTION_TEXT = Pattern.compile(
            "\\\"[^\\\"]*(?:desc|description|guide|subject|landing_text)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\""
    );
    private static final List<String> FORBIDDEN_NAMES = List.of(
            "Aquamirae",
            "Cataclysm",
            "Create",
            "JEI"
    );

    @Test
    void descriptionsDoNotNameModsOrIntegrations() throws IOException {
        assertClean("en_us.json");
        assertClean("ru_ru.json");
    }

    private static void assertClean(String language) throws IOException {
        String resource = "/assets/jemcompat/lang/" + language;
        try (InputStream stream = PlayerFacingTextPolicyTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            Matcher matcher = DESCRIPTION_TEXT.matcher(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            while (matcher.find()) {
                String value = matcher.group(1);
                FORBIDDEN_NAMES.forEach(name -> assertFalse(value.contains(name), value));
            }
        }
    }
}
