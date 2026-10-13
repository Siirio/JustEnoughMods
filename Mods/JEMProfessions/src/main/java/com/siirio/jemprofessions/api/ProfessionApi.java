package com.siirio.jemprofessions.api;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import net.minecraftforge.fml.ModList;
import java.util.Arrays;

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
        if (result.currentLevel() > result.previousLevel()) {
            ProfessionCatalog.nodes(request.profession()).stream()
                    .filter(node -> node.level() > result.previousLevel() && node.level() <= result.currentLevel())
                    .forEach(node -> MinecraftForge.EVENT_BUS.post(new PerkUnlockedEvent(player, request.profession(), node.id(), node.level())));
        }
        return result;
    }

    public static void administrativeCredit(ServerPlayer player, ProfessionId id, int delta) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty() || delta <= 0) return;
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        data.record(principal.get().accountId()).administrativeCredit(id, delta);
        data.changed();
    }

    public static int level(ServerPlayer player, ProfessionId id) {
        var principal = IdentityPolicy.resolve(player);
        return principal.map(value -> ProfessionSavedData.get(player.getServer()).findOrEmpty(value.accountId()).level(id)).orElse(1);
    }

    public static boolean toggle(ServerPlayer player, ProfessionId profession, String nodeId) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return false;
        var node = ProfessionCatalog.nodes(profession).stream().filter(value -> value.id().equals(nodeId)).findFirst().orElse(null);
        if (node == null || !node.toggleable() || level(player, profession) < node.level()) return false;
        if (!node.dependency().isBlank() && !ModList.get().isLoaded(node.dependency())) return false;
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        data.record(principal.get().accountId()).toggle(profession.name() + ":" + nodeId);
        data.changed();
        return true;
    }

    public static boolean enabled(ServerPlayer player, ProfessionId profession, String nodeId) {
        var principal = IdentityPolicy.resolve(player);
        return principal.map(value -> ProfessionSavedData.get(player.getServer()).findOrEmpty(value.accountId())
                .enabled(profession.name() + ":" + nodeId)).orElse(false);
    }
}
