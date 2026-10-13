package com.siirio.jemeconomy.api;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class EconomyApi {
    private static final long STARTER_GRANT = 100L;

    private EconomyApi() {
    }

    public static long balance(ServerPlayer player) {
        if (!DedicatedSmp.active(player)) return 0L;
        return IdentityPolicy.resolve(player).map(principal -> {
            ensureStarter(player, principal.accountId());
            return EconomySavedData.get(player.getServer()).balance(principal.accountId());
        }).orElse(0L);
    }

    public static TransferResult send(ServerPlayer sender, UUID recipient, long amount, UUID requestId) {
        if (!DedicatedSmp.active(sender)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        var principal = IdentityPolicy.resolve(sender);
        if (principal.isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        ensureStarter(sender, principal.get().accountId());
        return EconomySavedData.get(sender.getServer()).transfer(principal.get().accountId(), recipient, amount,
                "playerSend:" + principal.get().accountId() + ":" + requestId);
    }

    public static TransferResult fundedSystemTransfer(ServerPlayer authenticatedActor, SystemAccount source, UUID destination,
                                                      long amount, String idempotencyKey) {
        if (!DedicatedSmp.active(authenticatedActor)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        if (IdentityPolicy.resolve(authenticatedActor).isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        return EconomySavedData.get(authenticatedActor.getServer()).transfer(source.id(), destination, amount, idempotencyKey);
    }

    private static void ensureStarter(ServerPlayer player, UUID accountId) {
        EconomySavedData.get(player.getServer()).transfer(SystemAccount.CENTRAL_RESERVE.id(), accountId, STARTER_GRANT,
                "starterGrant:" + accountId);
    }
}
