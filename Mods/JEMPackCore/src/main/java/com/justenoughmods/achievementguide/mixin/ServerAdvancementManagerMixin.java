package com.justenoughmods.achievementguide.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.siirio.jempackcore.JEMPackCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin {
    private static final AdvancementAllowlist JEM_ADVANCEMENT_ALLOWLIST = loadAllowlist();
    private static final ResourceLocation RETIRED_BOSS_ROOT = new ResourceLocation("jem_guide", "bosses/root");

    @ModifyVariable(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Map<ResourceLocation, JsonElement> jemAdvancements$removeForeignAdvancements(Map<ResourceLocation, JsonElement> resources) {
        Map<ResourceLocation, JsonElement> filtered = new LinkedHashMap<>();
        resources.forEach((id, json) -> {
            if (RETIRED_BOSS_ROOT.equals(id)) {
                return;
            }
            if (isOwnedNamespace(id)) {
                if (isBundledAdvancement(id)) {
                    filtered.put(id, json);
                }
            } else if (JEM_ADVANCEMENT_ALLOWLIST.namespaces().contains(id.getNamespace())
                    || JEM_ADVANCEMENT_ALLOWLIST.advancements().contains(id.toString())) {
                JsonElement placement = removeRetiredBossBranch(json);
                if (placement != null) {
                    filtered.put(id, placement);
                }
            } else if (JEM_ADVANCEMENT_ALLOWLIST.compatibilityPlaceholders().contains(id.toString())) {
                filtered.put(id, compatibilityPlaceholder(json));
            }
        });
        return filtered;
    }

    private boolean isOwnedNamespace(ResourceLocation id) {
        return "jem".equals(id.getNamespace()) || "jem_guide".equals(id.getNamespace());
    }

    private boolean isBundledAdvancement(ResourceLocation id) {
        String path = "/data/" + id.getNamespace() + "/advancements/" + id.getPath() + ".json";
        return JEMPackCore.class.getResource(path) != null;
    }

    private JsonElement removeRetiredBossBranch(JsonElement original) {
        if (!original.isJsonObject()) {
            return original;
        }
        JsonObject object = original.getAsJsonObject();
        if (!object.has("parent") || !RETIRED_BOSS_ROOT.toString().equals(object.get("parent").getAsString())) {
            return original;
        }
        return null;
    }

    private JsonElement compatibilityPlaceholder(JsonElement original) {
        JsonObject placeholder = new JsonObject();
        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:impossible");
        JsonObject criteria = new JsonObject();
        criteria.add("compatibility", criterion);
        placeholder.add("criteria", criteria);
        JsonArray requirement = new JsonArray();
        requirement.add("compatibility");
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);
        placeholder.add("requirements", requirements);
        if (original.isJsonObject() && original.getAsJsonObject().has("rewards")) {
            placeholder.add("rewards", original.getAsJsonObject().get("rewards").deepCopy());
        }
        return placeholder;
    }

    private static AdvancementAllowlist loadAllowlist() {
        try (InputStream stream = JEMPackCore.class.getResourceAsStream("/data/jem_advancements/advancement_allowlist.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing JEM advancement allowlist");
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                AdvancementAllowlist value = new Gson().fromJson(reader, AdvancementAllowlist.class);
                return new AdvancementAllowlist(
                        Set.copyOf(value.namespaces()),
                        Set.copyOf(value.advancements()),
                        Set.copyOf(value.compatibilityPlaceholders())
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load JEM advancement allowlist", exception);
        }
    }

    private record AdvancementAllowlist(
            Set<String> namespaces,
            Set<String> advancements,
            @SerializedName("compatibility_placeholders")
            Set<String> compatibilityPlaceholders
    ) {
        private AdvancementAllowlist {
            namespaces = namespaces == null ? Set.of() : namespaces;
            advancements = advancements == null ? Set.of() : advancements;
            compatibilityPlaceholders = compatibilityPlaceholders == null ? Set.of() : compatibilityPlaceholders;
        }
    }
}
