package com.siirio.jemeconomy.ledger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public enum SystemAccount {
    CENTRAL_RESERVE(60_000L),
    NPC_MARKET(15_000L),
    OPERATIONS(20_000L),
    CONTINGENCY(5_000L);

    private final UUID id;
    private final long initialBalance;

    SystemAccount(long initialBalance) {
        this.id = UUID.nameUUIDFromBytes(("jem:economy:" + name()).getBytes(StandardCharsets.UTF_8));
        this.initialBalance = initialBalance;
    }

    public UUID id() { return id; }
    public long initialBalance() { return initialBalance; }
}
