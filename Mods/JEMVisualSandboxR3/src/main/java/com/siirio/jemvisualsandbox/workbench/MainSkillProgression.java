package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.server.level.ServerPlayer;

/** Ordered sandbox unlocks. Production progression remains owned by JEMRoles. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class MainSkillProgression {
    private MainSkillProgression() {}
    public static int level(ServerPlayer p,int race) { return Math.max(0,Math.min(3,p.getPersistentData().getInt("JemSandboxMainUpgrade"+race))); }
    static void unlockNext(ServerPlayer p,int requested) {
        int race=p.getPersistentData().getInt("JemSandboxRace");
        if(p.server.isDedicatedServer() || !p.server.isSingleplayerOwner(p.getGameProfile()) || !p.isAlive() || race!=2 || requested!=level(p,race)+1 || requested>3)return;
        p.getPersistentData().putInt("JemSandboxMainUpgrade"+race,requested);SandboxNetwork.status(p);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone e){
        if(e.getEntity().getServer()==null || e.getEntity().getServer().isDedicatedServer())return;
        e.getEntity().getPersistentData().putInt("JemSandboxMainUpgrade2",Math.max(0,Math.min(3,e.getOriginal().getPersistentData().getInt("JemSandboxMainUpgrade2"))));
    }
}
