package com.siirio.jemvisualsandbox.client.races;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Restarts Quick Skin's own session once ServerLogin has admitted this connection. */
public final class QuickSkinSessionResume {
    private static final Logger LOGGER=LoggerFactory.getLogger(QuickSkinSessionResume.class);

    private QuickSkinSessionResume() {}

    public static void resume(){
        Minecraft client=Minecraft.getInstance();
        if(client.player==null||client.getConnection()==null||!ModList.get().isLoaded("quickskin"))return;
        String version=ModList.get().getModContainerById("quickskin").orElseThrow().getModInfo().getVersion().toString();
        Object connection=client.getConnection();
        try {
            if("2.6.2.4".equals(version)){
                resumeLegacy(client.player.getUUID());
                return;
            }
            if(!"3.1.1".equals(version))return;
            ClassLoader loader=QuickSkinSessionResume.class.getClassLoader();
            Class<?> sessionsType=Class.forName("com.quickskin.mod.networking.protocol.ProtocolSessions",false,loader);
            Object sessions=sessionsType.getMethod("getInstance").invoke(null);
            Object profile=sessionsType.getMethod("clientProfile",Object.class).invoke(sessions,connection);
            Object mode=profile.getClass().getMethod("mode").invoke(profile);
            if(!(mode instanceof Enum<?> value)||!"LOCAL_ONLY".equals(value.name()))return;
            Class<?> syncType=Class.forName("com.quickskin.mod.networking.NetworkSyncService",false,loader);
            Object sync=syncType.getMethod("getInstance").invoke(null);
            syncType.getMethod("beginAppearanceSnapshotRequest",UUID.class,Object.class)
                    .invoke(sync,client.player.getUUID(),connection);
            LOGGER.info("Restarted native Quick Skin session after authentication");
        } catch(ReflectiveOperationException|LinkageError|ClassCastException failure){
            LOGGER.warn("Unable to resume native Quick Skin session after authentication",failure);
        }
    }

    private static void resumeLegacy(UUID playerId) throws ReflectiveOperationException {
        ClassLoader loader=QuickSkinSessionResume.class.getClassLoader();
        Class<?> repositoryType=Class.forName("com.quickskin.mod.common.data.PlayerAppearanceRepository",false,loader);
        Object repository=repositoryType.getMethod("getInstance").invoke(null);
        Object appearance=repositoryType.getMethod("getAppearance",UUID.class).invoke(repository,playerId);
        if(appearance==null)return;
        Class<?> appearanceType=appearance.getClass();
        String skin=(String)appearanceType.getMethod("getSkinId").invoke(appearance);
        String cape=(String)appearanceType.getMethod("getCapeId").invoke(appearance);
        String model=(String)appearanceType.getMethod("getModel").invoke(appearance);
        Class<?> syncType=Class.forName("com.quickskin.mod.networking.NetworkSyncService",false,loader);
        Object sync=syncType.getMethod("getInstance").invoke(null);
        syncType.getMethod("syncAppearance",UUID.class,String.class,String.class,String.class)
                .invoke(sync,playerId,skin,cape,model);
        LOGGER.info("Replayed native Quick Skin appearance after authentication");
    }
}
