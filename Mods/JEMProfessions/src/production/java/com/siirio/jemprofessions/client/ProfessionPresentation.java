package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.ProfessionStatusPacket;
public final class ProfessionPresentation {
    private ProfessionPresentation() {}
    public static void accept(ProfessionStatusPacket packet) {
        if (packet.open()) net.minecraft.client.Minecraft.getInstance().setScreen(new ProfessionRadialScreen(packet));
    }
}
