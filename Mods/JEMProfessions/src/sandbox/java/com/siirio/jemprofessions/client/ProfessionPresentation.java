package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.ProfessionStatusPacket;
import net.minecraft.client.Minecraft;
public final class ProfessionPresentation {
    private ProfessionPresentation() {}
    public static void accept(ProfessionStatusPacket packet) {
        SandboxClientState.progress(packet);
        if (Minecraft.getInstance().screen instanceof ProfessionRadialScreen screen) screen.accept(packet);
        else if (packet.open()) Minecraft.getInstance().setScreen(new ProfessionRadialScreen(packet));
    }
}
