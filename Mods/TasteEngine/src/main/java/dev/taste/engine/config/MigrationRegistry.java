package dev.taste.engine.config;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class MigrationRegistry {
    private final Map<Integer, UnaryOperator<JsonObject>> migrations = new LinkedHashMap<>();
    public void register(int sourceVersion, UnaryOperator<JsonObject> migration) { if(migrations.putIfAbsent(sourceVersion,migration)!=null)throw new IllegalArgumentException("Duplicate migration"); }
    public JsonObject migrate(JsonObject input, int targetVersion) {
        JsonObject current=input.deepCopy();int version=current.get("schemaVersion").getAsInt();
        while(version<targetVersion){UnaryOperator<JsonObject> migration=migrations.get(version);if(migration==null)throw new IllegalStateException("No migration from "+version);current=migration.apply(current);version++;current.addProperty("schemaVersion",version);}return current;
    }
}
