package com.siirio.jemvisualsandbox.client.races;

import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import com.siirio.jemvisualsandbox.workbench.SandboxNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Presentation of server snapshots; never grants a race, tier or successful use. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="jem_visual_sandbox",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class RaceExperienceClient {
    private static RaceExperience.Status status;
    private static ClientLevel world;
    private static long receivedAt;
    private static int pendingView;
    private static boolean pendingMandatory;

    private RaceExperienceClient() {}

    public static void receive(RaceExperience.Status value) {
        Minecraft client = Minecraft.getInstance();
        world = client.level;
        status = value;
        receivedAt = client.level == null ? 0 : client.level.getGameTime();
        if (client.screen instanceof RaceExperienceScreen screen) screen.receive(value);
        else if (pendingView != 0) {
            int view = pendingView;
            pendingView = 0;
            client.setScreen(new RaceExperienceScreen(value, pendingMandatory || !value.selected(), view == 2 && value.selected()));
        }
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (world != client.level) {
            world = client.level;
            status = null;
            pendingView = 0;
        }
        if (client.player != null && status != null && !status.selected()
                && !(client.screen instanceof RaceExperienceScreen)) openSelection(true);
    }

    public static void openSelection(boolean mandatory) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (status == null) { pendingView = 1; pendingMandatory = mandatory; SandboxNetwork.sync(); return; }
        client.setScreen(new RaceExperienceScreen(status, mandatory || !status.selected(), false));
    }

    public static void openProgression() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (status == null) { pendingView = 2; pendingMandatory = false; SandboxNetwork.sync(); return; }
        client.setScreen(new RaceExperienceScreen(status, !status.selected(), status.selected()));
    }

    public static boolean active() {
        return Minecraft.getInstance().screen instanceof RaceExperienceScreen;
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void nativeMenu(net.minecraftforge.client.event.ScreenEvent.Opening event){
        var c=Minecraft.getInstance();
        if(event.getNewScreen()==null||!event.getNewScreen().getClass().getName().equals("com.siirio.jemroles.client.RaceSelectionScreen")
                ||c.getConnection()==null||!SandboxNetwork.supported(c.getConnection().getConnection()))return;
        if(status!=null)event.setNewScreen(new RaceExperienceScreen(status,!status.selected(),false));
        else {pendingView=1;pendingMandatory=true;event.setNewScreen(null);SandboxNetwork.sync();}
    }

    public static RaceExperience.Status status(){return status;}
    public static long remaining(long serverTicks) {
        Minecraft client = Minecraft.getInstance();
        return Math.max(0, serverTicks - (client.level == null ? 0 : client.level.getGameTime() - receivedAt));
    }
}
