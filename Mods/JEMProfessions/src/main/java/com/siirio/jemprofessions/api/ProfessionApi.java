package com.siirio.jemprofessions.api;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class ProfessionApi {
    private ProfessionApi() {
    }

    public static ProfessionProgressResult credit(ServerPlayer player, ProfessionProgressRequest request) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new ProfessionProgressResult(ProfessionProgressResult.Status.UNAUTHENTICATED, 1, 1, 0L);
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        var record = data.record(principal.get().accountId());
        int before = record.level(request.profession());
        if (!record.credit(request.profession(), request.baseDelta(), request.auditId())) {
            return new ProfessionProgressResult(ProfessionProgressResult.Status.DUPLICATE, before, before, record.xp(request.profession()));
        }
        data.changed();
        ProfessionProgressResult result = new ProfessionProgressResult(ProfessionProgressResult.Status.ACCEPTED, before,
                record.level(request.profession()), record.xp(request.profession()));
        MinecraftForge.EVENT_BUS.post(new ProfessionProgressEvent(player, request, result));
        return result;
    }

    public static void administrativeCredit(ServerPlayer player, ProfessionId id, int delta) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty() || delta <= 0) return;
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        data.record(principal.get().accountId()).administrativeCredit(id, delta);
        data.changed();
    }
}
