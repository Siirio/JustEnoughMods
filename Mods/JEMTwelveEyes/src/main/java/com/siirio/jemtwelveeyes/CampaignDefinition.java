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
        Path config = FMLPaths.CONFIGDIR.get().resolve("jem_twelve_eyes-campaigns.json");
        if (Files.isRegularFile(config)) {
            return read(config);
        }
        var definitions = ModList.get().getModFiles().stream()
                .map(file -> file.getFile().findResource(RESOURCE))
                .filter(Files::isRegularFile).toList();
        if (definitions.size() > 1) {
            throw new IllegalStateException("Multiple campaign definitions; select one in " + config);
        }
        if (!definitions.isEmpty()) {
            return read(definitions.get(0));
        }
        var empty = new JsonObject();
        empty.add("bosses", new JsonArray());
        empty.add("prerequisites", new JsonArray());
        return empty;
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
