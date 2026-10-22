package com.siirio.jemsettlements.currency;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Stable settlement financial destination IDs; these do not create or own Coin balances. */
public enum SettlementAccounts {
    CENTRAL_RESERVE, OPERATIONS, NPC_MARKET, MARKET_ESCROW;
    public UUID id() { return UUID.nameUUIDFromBytes(("jem:economy:" + name()).getBytes(StandardCharsets.UTF_8)); }
}
