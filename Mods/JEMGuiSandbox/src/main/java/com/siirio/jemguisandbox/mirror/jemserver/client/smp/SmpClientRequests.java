package com.siirio.jemguisandbox.mirror.jemserver.client.smp;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.siirio.jemguisandbox.mirror.jemserver.smp.SmpAction;
final class SmpClientRequests {
    static SmpAction action(UUID request, String table, UUID id, int revision, String action, JsonObject fields) {
        return new SmpAction(request, table, id, revision, new SmpAction.Kind(action), fields.deepCopy());
    }
}
