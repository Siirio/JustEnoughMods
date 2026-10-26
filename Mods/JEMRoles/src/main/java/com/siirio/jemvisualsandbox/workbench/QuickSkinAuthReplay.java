package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Asks the installed Quick Skin owner to resend its roster after ServerLogin authentication. */
final class QuickSkinAuthReplay {
    private static final Logger LOGGER=LoggerFactory.getLogger(QuickSkinAuthReplay.class);

    private QuickSkinAuthReplay() {}

    static void replayLegacyRoster(ServerPlayer player){
        var installed=ModList.get().getModContainerById("quickskin");
        if(installed.isEmpty()||!"2.6.2.4".equals(installed.get().getModInfo().getVersion().toString()))return;
        try {
            Class<?> handler=Class.forName("com.quickskin.mod.networking.ServerNetworkHandler");
            handler.getMethod("sendAllAppearancesToPlayer",ServerPlayer.class).invoke(null,player);
            handler.getMethod("sendServerConfigToPlayer",ServerPlayer.class).invoke(null,player);
            LOGGER.info("Replayed native Quick Skin roster after authentication");
        } catch(ReflectiveOperationException|LinkageError failure){
            LOGGER.warn("Unable to replay native Quick Skin roster after authentication",failure);
        }
    }
}
