package dev.taste.engine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class PresetCodec {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public String encode(String name, ConfigSnapshot snapshot){JsonObject root=new JsonObject();root.addProperty("schemaVersion",1);root.addProperty("name",name);root.addProperty("style",snapshot.style());root.addProperty("quality",snapshot.quality());JsonObject values=new JsonObject();snapshot.userOverrides().forEach(values::add);root.add("values",values);return GSON.toJson(root);}
    public JsonObject decode(String json){JsonObject root=JsonParser.parseString(json).getAsJsonObject();if(root.get("schemaVersion").getAsInt()!=1||!root.has("name")||!root.has("values"))throw new IllegalArgumentException("Invalid Taste preset");return root;}
}
