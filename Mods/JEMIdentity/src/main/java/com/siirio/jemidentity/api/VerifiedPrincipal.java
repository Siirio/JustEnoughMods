package com.siirio.jemidentity.api;

import java.util.UUID;

public record VerifiedPrincipal(UUID accountId, PrincipalKind kind) {
    public VerifiedPrincipal {
        if (accountId == null || kind == null) throw new IllegalArgumentException("Verified principal fields are required");
    }
}
