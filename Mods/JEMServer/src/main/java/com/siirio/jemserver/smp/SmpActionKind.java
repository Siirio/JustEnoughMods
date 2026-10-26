package com.siirio.jemserver.smp;

import java.util.Locale;

public enum SmpActionKind {
    CLAIM_REWARDS, CLAIM, CLAIM_ALL, CREATE,
    NAVIGATE, TELEPORT_EVENT, TPA, MESSAGE, JOIN_RAID, CREATE_PARTY,
    SOLO, INVITE, MULTIPLAYER,
    JOIN, ARRIVE, LEAVE, READY, PUBLISH, APPROVE, REMOVE, TRANSFER,
    CANCEL, DISSOLVE, START, HOME, WALLET, DEATH_HISTORY;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SmpActionKind fromKey(String key) {
        return valueOf(key.toUpperCase(Locale.ROOT));
    }

    public boolean playerInput(SmpTable table) {
        return this == APPROVE || this == REMOVE || this == TRANSFER
                || this == INVITE && table == SmpTable.PARTIES;
    }
}
