package dev.taste.engine.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

public final class MaterialRuleEditor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<JsonObject> rules = new ArrayList<>();
    public void load(String json) {
        rules.clear();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        root.getAsJsonArray("rules").forEach(element -> rules.add(element.getAsJsonObject().deepCopy()));
    }
    public void add(JsonObject rule) { rules.add(rule.deepCopy()); }
    public void remove(int index) { rules.remove(index); }
    public String save() { JsonObject root=new JsonObject();com.google.gson.JsonArray array=new com.google.gson.JsonArray();rules.forEach(array::add);root.add("rules",array);return GSON.toJson(root); }
    public List<JsonObject> rules() { return rules.stream().map(JsonObject::deepCopy).toList(); }
}
