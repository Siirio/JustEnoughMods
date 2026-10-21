package com.siirio.jemserver.client.xaero;

import com.siirio.jemserver.Landmarks;
import com.siirio.jemserver.MapNetwork;
import com.siirio.jemserver.client.XaeroLandmarks;

/** Native Xaero actions resolve only object identities created from server snapshots. */
public final class OfficialMarkerTravel {
    public static boolean available(Object waypoint) {
        if (!MapClient.connected()) return false;
        var boss = XaeroLandmarks.shared(waypoint);
        return (boss != null && Landmarks.isBoss(boss)) || XaeroLandmarks.event(waypoint) != null;
    }

    public static boolean request(Object waypoint) {
        if (!available(waypoint)) return false;
        var boss = XaeroLandmarks.shared(waypoint);
        if (boss != null && Landmarks.isBoss(boss)) MapNetwork.teleportBoss(boss.id());
        else MapNetwork.teleportEvent(XaeroLandmarks.event(waypoint));
        net.minecraft.client.Minecraft.getInstance().setScreen(null);
        return true;
    }

    private OfficialMarkerTravel() {}
}
