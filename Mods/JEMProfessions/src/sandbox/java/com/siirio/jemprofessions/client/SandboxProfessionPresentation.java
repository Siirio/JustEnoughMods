package com.siirio.jemprofessions.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.siirio.jemprofessions.tree.ProfessionNode;

/** Sandbox-only icon adaptation and truthful availability; it never owns unlock/progress state. */
final class SandboxProfessionPresentation {
    private static final JsonObject ICONS;
    static {
        try (var stream=SandboxProfessionPresentation.class.getResourceAsStream("/assets/jem_professions/sandbox-icons.json")) {
            if (stream==null) throw new IllegalStateException("Sandbox profession icon mapping is missing");
            ICONS=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private SandboxProfessionPresentation() { }
    static String icon(String id) { return ICONS.has(id)?ICONS.get(id).getAsString():id; }
    static boolean available(ProfessionNode node) {
        return com.siirio.jemprofessions.sandbox.SandboxProfessionPolicy.available(node.id());
    }
}
