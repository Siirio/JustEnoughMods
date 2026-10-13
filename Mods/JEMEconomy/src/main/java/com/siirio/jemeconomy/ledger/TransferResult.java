package com.siirio.jemeconomy.ledger;

import java.util.UUID;

public record TransferResult(Status status, UUID receiptId, long sourceBalance, long destinationBalance) {
    public enum Status { COMMITTED, REPLAYED, INSUFFICIENT_FUNDS, UNAUTHENTICATED, INVALID, SERVER_ONLY }
}
