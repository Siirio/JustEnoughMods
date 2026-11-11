package dev.taste.engine.material;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** Loads original Taste material rules from data resources. */
public final class MaterialRuleLoader {
    private static final Gson GSON = new Gson();
    public int load(ResourceManager resources, MaterialResolver resolver) throws IOException {
        Map<ResourceLocation, Resource> files = resources.listResources("materials", id -> id.getPath().endsWith(".json"));
        int count = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int priority = root.has("priority") ? root.get("priority").getAsInt() : 0;
                ResourceLocation block = root.has("block") ? new ResourceLocation(root.get("block").getAsString()) : null;
                ResourceLocation sprite = root.has("sprite") ? new ResourceLocation(root.get("sprite").getAsString()) : null;
                MaterialDescription material = GSON.fromJson(root.getAsJsonObject("material"), MaterialDescription.class);
                resolver.add(priority, query -> (block == null || block.equals(query.block()))
                        && (sprite == null || sprite.equals(query.sprite())) ? java.util.Optional.of(material) : java.util.Optional.empty());
                count++;
            }
        }
        return count;
    }
}
