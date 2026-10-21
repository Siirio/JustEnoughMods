package com.siirio.jemroles.trait;

import com.siirio.jemroles.network.RaceActionPacket;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import com.siirio.jemvisualsandbox.workbench.SandboxOptionalIrons;
import net.minecraft.server.level.ServerPlayer;

/** Legacy public entry points delegate to the sole final mechanics owner. */
public final class RaceAbilities {
    private RaceAbilities() {}
    public static void handle(ServerPlayer player, RaceActionPacket.Action action) {
        if (action == RaceActionPacket.Action.SIGNATURE) RaceExperience.cast(player);
        else RaceExperience.utility(player, action == RaceActionPacket.Action.UTILITY_PRESS);
    }
    public static boolean castSignature(ServerPlayer player) { return RaceExperience.cast(player); }
    public static boolean canCastSignature(ServerPlayer player) {
        return RaceExperience.canAct(player) && RaceExperience.ready(player, "signature");
    }
    public static int signatureCooldown(RaceId race) { return RaceExperience.signatureCooldown(race.ordinal()); }
    public static void syncSignatureCooldown(ServerPlayer player) { SandboxOptionalIrons.sync(player); }
    public static void logout(ServerPlayer player) { RaceExperience.selectionChanged(player); }
}
