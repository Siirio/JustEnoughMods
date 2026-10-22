package com.siirio.jemvisualsandbox.client.races;

import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import com.siirio.jemvisualsandbox.workbench.SandboxNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Presentation of server snapshots; never grants a race, tier or successful use. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="jem_roles",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class RaceExperienceClient {
    private static RaceExperience.Status status;
    private static net.minecraft.client.multiplayer.ClientPacketListener connection;
    private static long receivedAt;
    private static int pendingView;
    private static boolean pendingMandatory;

    private RaceExperienceClient() {}
    public static boolean supported(){var c=Minecraft.getInstance();return c.getConnection()!=null && SandboxNetwork.supported(c.getConnection().getConnection());}

    public static void receive(RaceExperience.Status value) {
        Minecraft client = Minecraft.getInstance();
        resetConnection(client);
        status = value;
        receivedAt = client.level == null ? 0 : client.level.getGameTime();
        if (client.screen instanceof RaceExperienceScreen screen) screen.receive(value);
        else if (!value.selected()) { pendingView = 1; pendingMandatory = true; }
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        resetConnection(client);
        // A ready authenticated snapshot may queue a choice, never replace login/loading screens.
        if (client.player == null || client.level == null || status == null || client.screen != null) return;
        if (!status.selected()) { pendingView = 1; pendingMandatory = true; }
        if (pendingView != 0) {
            int view = pendingView;
            pendingView = 0;
            client.setScreen(new RaceExperienceScreen(status, pendingMandatory || !status.selected(), view == 2 && status.selected()));
            pendingMandatory = false;
        }
    }

    private static void resetConnection(Minecraft client) {
        if (connection == client.getConnection()) return;
        connection = client.getConnection();
        status = null;
        pendingView = 0;
        pendingMandatory = false;
        receivedAt = 0L;
    }

    public static void openSelection(boolean mandatory) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (client.screen instanceof RaceExperienceScreen) return;
        pendingView = 1; pendingMandatory = mandatory;
        if (status == null) SandboxNetwork.sync();
        else tick();
    }

    public static void openProgression() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (client.screen instanceof RaceExperienceScreen) return;
        pendingView = 2; pendingMandatory = false;
        if (status == null) SandboxNetwork.sync();
        else tick();
    }

    public static boolean active() {
        return Minecraft.getInstance().screen instanceof RaceExperienceScreen;
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void nativeMenu(net.minecraftforge.client.event.ScreenEvent.Opening event){
        var c=Minecraft.getInstance();
        if(event.getNewScreen()==null||!event.getNewScreen().getClass().getName().equals("com.siirio.jemroles.client.RaceSelectionScreen")
                ||c.getConnection()==null||!SandboxNetwork.supported(c.getConnection().getConnection()))return;
        if(c.screen instanceof RaceExperienceScreen current)event.setNewScreen(current);
        else if(status!=null&&c.screen==null)event.setNewScreen(new RaceExperienceScreen(status,!status.selected(),false));
        else {pendingView=1;pendingMandatory=true;event.setNewScreen(null);SandboxNetwork.sync();}
    }

    public static RaceExperience.Status status(){return status;}
    public static long remaining(long serverTicks) {
        Minecraft client = Minecraft.getInstance();
        return Math.max(0, serverTicks - (client.level == null ? 0 : client.level.getGameTime() - receivedAt));
    }
}
