package com.siirio.jemcompat.compat.jade;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TamingCatalogTest {
    @Test
    void returnsConfiguredTamingItemsInDeclaredOrder() {
        TamingCatalog catalog = TamingCatalog.parse(JsonParser.parseString("""
                {
                  "entries": [
                    {
                      "entity": "minecraft:cat",
                      "items": ["minecraft:cod", "minecraft:salmon"]
                    }
                  ]
                }
                """).getAsJsonObject());

        assertEquals(List.of(
                new ResourceLocation("minecraft", "cod"),
                new ResourceLocation("minecraft", "salmon")
        ), catalog.itemsFor(new ResourceLocation("minecraft", "cat")));
    }

    @Test
    void omitsInformationForUnconfiguredEntities() {
        TamingCatalog catalog = TamingCatalog.parse(JsonParser.parseString("{\"entries\":[]}").getAsJsonObject());

        assertTrue(catalog.itemsFor(new ResourceLocation("minecraft", "cow")).isEmpty());
    }
}
