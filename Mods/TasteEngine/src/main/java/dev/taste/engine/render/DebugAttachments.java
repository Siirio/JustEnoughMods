package dev.taste.engine.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Frame-local debug labels. Texture IDs are diagnostic only and never become ownership handles. */
public final class DebugAttachments {
    public record Attachment(int textureId, int width, int height, String description) { }
    private final Map<String, Attachment> attachments = new LinkedHashMap<>();
    public void publish(String name, Attachment attachment) { attachments.put(name, attachment); }
    public Map<String, Attachment> snapshot() { return Map.copyOf(attachments); }
    public void beginFrame() { attachments.clear(); }
}
