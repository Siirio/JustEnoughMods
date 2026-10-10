package com.siirio.jemserver.smp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class SmpActionSession {
    final Map<UUID, SmpReceipt> receipts = new LinkedHashMap<>();
    long actedAt = Long.MIN_VALUE;
    long revivedAt = Long.MIN_VALUE;

    void remember(SmpAction action, String error) {
        receipts.put(action.request(), new SmpReceipt(action, error));
        while (receipts.size() > SmpProtocol.RECEIPTS) receipts.remove(receipts.keySet().iterator().next());
    }
}
