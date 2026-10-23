package com.siirio.jemcompat.feature.shelter;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;

/** Fail closed for a newly installed claims implementation until its real animal-spawn permission API is adapted. */
final class ShelterProtection {
    private ShelterProtection() { }
    static boolean recoveryAllowed(ServerLevel level, UUID owner, BlockPos pos) {
        // None of these claim implementations is in the inspected ManagedPack. Native placement/break/use events
        // remain in charge of actual player edits; a synthetic click must never execute another mod's block action.
        if (ModList.get().isLoaded("flan") || ModList.get().isLoaded("ftbchunks") || ModList.get().isLoaded("openpartiesandclaims")) return false;
        var player = level.getServer().getPlayerList().getPlayer(owner);
        // Offline owners cannot supply an authenticated native permission context. Preserve the recovery WAL.
        return player != null && !level.getServer().isUnderSpawnProtection(level, pos, player);
    }
}
