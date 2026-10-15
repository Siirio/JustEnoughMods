package com.siirio.jemprofessions.api;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.JemProfessions;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

public final class ProfessionApi {
    private ProfessionApi() {
    }

    public static void openScreen(ServerPlayer player) {
        if (IdentityPolicy.resolve(player).isPresent()) com.siirio.jemprofessions.network.ProfessionsNetwork.sendStatus(player, true);
    }

    public static ProfessionProgressResult credit(ServerPlayer player, ProfessionProgressRequest request) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new ProfessionProgressResult(ProfessionProgressResult.Status.UNAUTHENTICATED, 1, 1, 0L);
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        var record = data.record(principal.get().accountId());
        int before = level(player, request.profession());
        if (!record.reserveAudit(request.auditId())) {
            return new ProfessionProgressResult(ProfessionProgressResult.Status.DUPLICATE, before, before, xp(player, request.profession()));
        }
        data.changed();
        experience(player, request.profession()).ifPresent(value -> value.addTotal(player, request.baseDelta()));
        ProfessionProgressResult result = new ProfessionProgressResult(ProfessionProgressResult.Status.ACCEPTED, before,
                level(player, request.profession()), xp(player, request.profession()));
        MinecraftForge.EVENT_BUS.post(new ProfessionProgressEvent(player, request, result));
        return result;
    }

    public static void administrativeCredit(ServerPlayer player, ProfessionId id, int delta) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty() || delta <= 0) return;
        experience(player, id).ifPresent(value -> value.addTotal(player, delta));
    }

    public static int level(ServerPlayer player, ProfessionId id) {
        return experience(player, id).map(value -> value.getLevel(player)).orElse(0);
    }

    public static long xp(ServerPlayer player, ProfessionId id) {
        return experience(player, id).map(value -> value.getTotal(player)).orElse(0);
    }

    public static boolean enabled(ServerPlayer player, ProfessionId profession, String nodeId) {
        return category(profession).flatMap(value -> value.getSkill(nodeId))
                .map(value -> value.getState(player) == Skill.State.UNLOCKED).orElse(false);
    }

    public static void migrateLegacyProgress(ServerPlayer player) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return;
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        var record = data.record(principal.get().accountId());
        for (ProfessionId id : ProfessionId.values()) {
            if (!record.markPuffishMigrated(id)) continue;
            long legacy = record.xp(id);
            experience(player, id).ifPresent(value -> value.setTotal(player,
                    (int) Math.min(Integer.MAX_VALUE, Math.max(legacy, value.getTotal(player)))));
            String prefix = id.name() + ":";
            for (String key : record.enabledKeys()) if (key.startsWith(prefix)) {
                category(id).flatMap(value -> value.getSkill(key.substring(prefix.length()))).ifPresent(skill -> skill.unlock(player));
            }
            data.changed();
        }
    }

    public static java.util.Optional<net.puffish.skillsmod.api.Category> category(ProfessionId id) {
        return SkillsAPI.getCategory(new ResourceLocation(JemProfessions.MOD_ID, id.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private static java.util.Optional<net.puffish.skillsmod.api.Experience> experience(ServerPlayer player, ProfessionId id) {
        if (IdentityPolicy.resolve(player).isEmpty()) return java.util.Optional.empty();
        return category(id).flatMap(net.puffish.skillsmod.api.Category::getExperience);
    }
}
