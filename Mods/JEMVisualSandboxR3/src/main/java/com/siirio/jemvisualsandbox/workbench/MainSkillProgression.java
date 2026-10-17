package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.server.level.ServerPlayer;

/** Ordered sandbox unlocks. Production progression remains owned by JEMRoles. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class MainSkillProgression {
    private MainSkillProgression() {}
    public static int level(ServerPlayer p,int race) { return race==2?RaceExperience.bloomUpgrade(p):RaceExperience.tier(p); }
    static void unlockNext(ServerPlayer p,int requested) {
        RaceExperience.upgrade(p,requested);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone e){
        if(e.getEntity().getServer()==null || e.getEntity().getServer().isDedicatedServer())return;
        e.getEntity().getPersistentData().putInt("JemSandboxMainUpgrade2",Math.max(0,Math.min(3,e.getOriginal().getPersistentData().getInt("JemSandboxMainUpgrade2"))));
    }
}
