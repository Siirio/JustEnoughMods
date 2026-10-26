package com.siirio.jemvisualsandbox.client.races;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One server-marked racial damage event is hidden per affected entity and viewer. */
@Mod.EventBusSubscriber(modid="jem_roles",value=Dist.CLIENT)
public final class RaceDamagePresentation {
    private static final Map<Integer,ClientLevel> ARMED=new HashMap<>();
    private RaceDamagePresentation() {}

    public static void receive(boolean enabled,int entityId) {
        if(!enabled) { ARMED.remove(entityId);return; }
        ClientLevel level=Minecraft.getInstance().level;
        if(level!=null && entityId>=0)ARMED.put(entityId,level);
    }

    public static boolean shouldSuppress(ClientboundDamageEventPacket packet,ClientLevel level) {
        int entityId=packet.entityId();
        if(level==null || ARMED.get(entityId)!=level || !packet.getSource(level).is(DamageTypes.DROWN))return false;
        ARMED.remove(entityId);
        return true;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ARMED.clear(); }
}
