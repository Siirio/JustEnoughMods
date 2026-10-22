package com.siirio.jemguisandbox.mirror.jemserver.smp;
import java.util.UUID;
import com.google.gson.JsonObject;
public record SmpAction(UUID request, String table, UUID id, int revision, Kind kind, JsonObject fields) {
    public record Kind(String key) { }
}
