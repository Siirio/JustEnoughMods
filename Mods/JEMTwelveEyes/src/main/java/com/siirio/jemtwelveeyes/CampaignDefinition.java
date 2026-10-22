package com.siirio.jemtwelveeyes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

public final class CampaignDefinition {
    private static final String RESOURCE = "jem_twelve_eyes/campaigns.json";
    private static final JsonObject DEFINITION = load();

    private CampaignDefinition() {
    }

    public static JsonArray entries(String name) {
        return DEFINITION.getAsJsonArray(name).deepCopy();
    }

    private static JsonObject load() {
        JsonObject original;
        try (var stream = CampaignDefinition.class.getResourceAsStream("/" + RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing original Twelve Eyes definition");
            original = JsonParser.parseReader(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read original Twelve Eyes definition", exception);
        }
        if (original.getAsJsonArray("bosses").size() != 12) {
            throw new IllegalStateException("The campaign requires exactly twelve original MAIN bosses");
        }
        Path config = FMLPaths.CONFIGDIR.get().resolve("jem_twelve_eyes-campaigns.json");
        if (Files.isRegularFile(config) && !original.equals(read(config))) {
            throw new IllegalStateException("Twelve Eyes cannot replace its original bosses, Eyes or prerequisites: " + config);
        }
        return original;
    }

    private static JsonObject read(Path path) {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var definition = JsonParser.parseReader(reader).getAsJsonObject();
            if (!definition.has("bosses") || !definition.has("prerequisites")) {
                throw new IllegalArgumentException("Campaign definition requires bosses and prerequisites: " + path);
            }
            return definition;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read campaign definition " + path, exception);
        }
    }
}
