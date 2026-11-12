package dev.taste.engine.config;

import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Pure policy only. The renderer must supply capabilities after validating its adapter and GL context. */
public record EffectiveConfig(ConfigSnapshot requested, Map<String, JsonPrimitive> values,
        Set<String> capabilities, Backend backend, String reason) {
    public enum Backend { OFF, VANILLA, EMBEDDIUM }
    public enum Embeddium { ABSENT, SUPPORTED_0_3_31, UNSUPPORTED }
    /** baselineValidated covers GLSL330, required MRT/FBO formats and per-stage sampler limits. */
    public record Capabilities(boolean rendererImplemented, boolean baselineValidated, Embeddium embeddium, Set<String> flags) {
        public Capabilities { flags = Set.copyOf(flags); }
        public static Capabilities foundation() { return new Capabilities(false, false, Embeddium.ABSENT, Set.of()); }
    }
    public EffectiveConfig { values = Map.copyOf(values); capabilities = Set.copyOf(capabilities); }

    public static EffectiveConfig resolve(ConfigSnapshot snapshot, Capabilities capabilities) {
        Map<String, JsonPrimitive> effective = new LinkedHashMap<>(snapshot.values());
        if (effective.get("gi.mode").getAsString().equals("voxel")
                && (!capabilities.flags().contains("gl43") || !capabilities.flags().contains("voxel_gi")))
            effective.put("gi.mode", new JsonPrimitive("screen"));
        Backend backend = Backend.OFF;
        String reason = "";
        String requestedBackend = snapshot.value("general.backend").getAsString();
        if (!capabilities.rendererImplemented()) reason = "Renderer implementation is unavailable.";
        else if (!capabilities.baselineValidated()) reason = "The render context has not completed the GLSL330 and framebuffer capability probe.";
        else if (!snapshot.requestedEnabled()) reason = "Master switch is Off.";
        else if (capabilities.embeddium() == Embeddium.UNSUPPORTED)
            reason = "Installed Embeddium is unsupported; remove it and restart or install the pinned supported version.";
        else if (capabilities.embeddium() == Embeddium.SUPPORTED_0_3_31) {
            if (requestedBackend.equals("vanilla")) reason = "Vanilla terrain cannot be selected while Embeddium is installed; remove it and restart.";
            else backend = Backend.EMBEDDIUM;
        } else if (requestedBackend.equals("embeddium")) reason = "Embeddium is not installed.";
        else backend = Backend.VANILLA;
        effective.put("general.enabled", new JsonPrimitive(backend != Backend.OFF));
        return new EffectiveConfig(snapshot, effective, capabilities.flags(), backend, reason);
    }
    public boolean active() { return backend != Backend.OFF; }
    public boolean available(SettingRegistry registry, String id) {
        return active() && registry.unmetRequirements(id, values, capabilities).isEmpty();
    }
}
