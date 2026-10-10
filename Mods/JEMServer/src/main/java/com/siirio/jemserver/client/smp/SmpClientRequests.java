package com.siirio.jemserver.client.smp;

import com.google.gson.JsonObject;
import com.siirio.jemserver.smp.*;
import java.util.UUID;

final class SmpClientRequests {
    static SmpAction action(UUID request, String tableKey, UUID id, int revision, String actionKey, JsonObject fields) {
        var table = SmpTable.fromKey(tableKey);
        var kind = SmpActionKind.fromKey(actionKey);
        SmpActionInput input = SmpNoInput.INSTANCE;
        if (kind == SmpActionKind.CREATE) {
            input = new SmpPartyRequest(SmpRecords.text(fields, "activity", SmpProtocol.MAX_LANGUAGE),
                    SmpRecords.text(fields, "title", SmpProtocol.MAX_TITLE),
                    SmpRecords.text(fields, "description", SmpProtocol.MAX_DESCRIPTION),
                    SmpRecords.text(fields, "language", SmpProtocol.MAX_LANGUAGE),
                    SmpRecords.number(fields, "slots", 0, 0, Integer.MAX_VALUE), SmpRecords.time(fields, "scheduled"),
                    fields.has("approval") && fields.get("approval").getAsBoolean(),
                    fields.has("solo") && fields.get("solo").getAsBoolean());
        } else if (kind == SmpActionKind.MESSAGE) {
            input = new SmpMessageRequest(SmpRecords.text(fields, "message", SmpProtocol.MAX_MESSAGE));
        } else if (kind.playerInput(table)) {
            input = new SmpPlayerRequest(UUID.fromString(SmpRecords.text(fields, "player", 36)));
        }
        return new SmpAction(request, table, id, revision, kind, input);
    }

    private SmpClientRequests() {}
}
