package com.siirio.jemprofessions.sandbox;

import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Server-local scope for the two trusted forced-unlock callers; never derived from a packet flag. */
public final class SandboxSkillAdmission {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private SandboxSkillAdmission() {}
    public static void freeClaim(ServerPlayer player, ProfessionId profession, String node, Runnable action) {
        if (!SandboxProfessionSupport.owner(player)) return;
        run(new Scope(player, profession, node, false), action);
    }
    public static void legacy(ServerPlayer player, Runnable action) {
        if (SandboxProfessionSupport.allowed(player)) run(new Scope(player, null, "", true), action);
    }
    private static void run(Scope scope, Runnable action) {
        Scope previous = ACTIVE.get();
        ACTIVE.set(scope);
        try { action.run(); } finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    public static boolean bypass(ServerPlayer player, ResourceLocation category, ProfessionId profession, String node) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.player != player || !category.getNamespace().equals("jem_professions")) return false;
        if (scope.legacy) return SandboxProfessionSupport.record(player).enabledKeys().contains(profession.name() + ":" + node);
        return SandboxProfessionSupport.owner(player) && scope.profession == profession && scope.node.equals(node);
    }
    private record Scope(ServerPlayer player, ProfessionId profession, String node, boolean legacy) {}
}
