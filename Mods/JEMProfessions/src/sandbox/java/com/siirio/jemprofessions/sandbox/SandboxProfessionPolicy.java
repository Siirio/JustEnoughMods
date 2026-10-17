package com.siirio.jemprofessions.sandbox;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Effect availability only; native Puffish keeps the sole graph, points and saved unlock state. */
public final class SandboxProfessionPolicy {
    private static final JsonObject SUPPORTED;
    static {
        try (var stream=SandboxProfessionPolicy.class.getResourceAsStream("/assets/jem_professions/sandbox-availability.json")) {
            if(stream==null) throw new IllegalStateException("Sandbox profession availability is missing");
            SUPPORTED=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(java.io.IOException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private SandboxProfessionPolicy() { }
    public static boolean available(String node) {
        if(!SUPPORTED.has(node))return false;
        String dependency=SUPPORTED.get(node).getAsString();
        return dependency.isBlank()||net.minecraftforge.fml.ModList.get().isLoaded(dependency);
    }
}
