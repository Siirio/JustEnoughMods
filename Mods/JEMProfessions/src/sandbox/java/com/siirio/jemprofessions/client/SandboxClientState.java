package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.sandbox.SandboxProfessionRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import java.util.HashSet;
import java.util.Set;

public final class SandboxClientState {
    private static String selected = "";
    private static String node = "";
    private static CompoundTag extra = new CompoundTag();
    private static final Set<String> TEST = new HashSet<>();
    private static boolean boatEligible;
    private static boolean owner;
    private static final Set<String> UNLOCKED=new HashSet<>();
    private SandboxClientState() {}
    public static void accept(SandboxProfessionRuntime.State state) {
        selected=state.selected(); node=state.node(); extra=state.extra().copy();
        if(extra.contains("Owner"))owner=extra.getBoolean("Owner");
        TEST.clear(); var claims=extra.getList("TestClaims",8);
        for(int i=0;i<claims.size();i++) TEST.add(claims.getString(i));
        if(Minecraft.getInstance().screen instanceof SandboxActionScreen screen) screen.accept(node,extra);
        if(Minecraft.getInstance().screen instanceof ProfessionRadialScreen screen)screen.refreshControls();
        if(extra.contains("OpenShop")) {
            var skill=com.siirio.jemprofessions.tree.ProfessionCatalog.node(com.siirio.jemprofessions.progress.ProfessionId.MERCHANT,"trading_post");
            Minecraft.getInstance().setScreen(new SandboxActionScreen(Minecraft.getInstance().screen,
                    com.siirio.jemprofessions.progress.ProfessionId.MERCHANT,skill).consumer(extra.getString("OpenShop")));
        }
    }
    public static void progress(com.siirio.jemprofessions.network.ProfessionStatusPacket packet) {
        boatEligible=packet.authenticated()&&packet.unlocked().contains("EXPLORER:river_runner");
        UNLOCKED.clear();for(String key:packet.unlocked()){int colon=key.indexOf(':');if(colon>=0)UNLOCKED.add(key.substring(colon+1));}
    }
    public static boolean boatEligible() { return boatEligible; }
    public static boolean owner(){return owner;}
    public static boolean unlocked(String node){return UNLOCKED.contains(node);}
    public static String selected() { return selected; }
    public static boolean testClaim(String id) { return TEST.contains(id); }
    public static CompoundTag extra(String id) { return node.equals(id)?extra.copy():new CompoundTag(); }
}
