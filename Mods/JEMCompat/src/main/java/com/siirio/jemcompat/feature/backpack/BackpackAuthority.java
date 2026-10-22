package com.siirio.jemcompat.feature.backpack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class BackpackAuthority {
    private BackpackAuthority() { }
    public static boolean permits(ServerPlayer player) {
        return player.getServer() != null && (!ModList.get().isLoaded("jem_identity") || Identity.permits(player));
    }
    private static final class Identity {
        private static boolean permits(ServerPlayer player) {
            return com.siirio.jemidentity.api.IdentityPolicy.permitsAccountMutation(player);
        }
    }
}
