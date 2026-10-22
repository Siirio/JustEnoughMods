package com.siirio.jemvisualsandbox.client.races;

import com.siirio.jemvisualsandbox.workbench.SandboxNetwork;
import com.siirio.jemvisualsandbox.workbench.SculkDomain;
import net.minecraft.client.Minecraft;

/** Presentation follows an integrated-server size decision, never computes territory authority. */
public final class SculkScaleClient {
    private SculkScaleClient() {}
    public static void receive(SandboxNetwork.SculkScale message) {
        var client=Minecraft.getInstance();
        if(client.level==null || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported()))return;
        var entity=client.level.getEntity(message.entityId());
        if(entity instanceof net.minecraft.world.entity.player.Player){
            entity.getPersistentData().putBoolean(SculkDomain.SCALE_KEY,message.enlarged());
            entity.refreshDimensions();
        }
    }
}
