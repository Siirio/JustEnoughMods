package com.siirio.jemprofessions.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemprofessions.JemProfessions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = JemProfessions.MOD_ID, value = Dist.CLIENT)
public final class SandboxProfessionKeys {
    private static final KeyMapping OPEN = new KeyMapping("key.jem_professions.open_sandbox", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7, "key.categories.jem_professions");
    public static final KeyMapping ACTION = new KeyMapping("key.jem_professions.action",InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),"key.categories.jem_professions");
    private static boolean holding;
    private SandboxProfessionKeys() { }
    @Mod.EventBusSubscriber(modid = JemProfessions.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        private Keys() { }
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN);event.register(ACTION); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN.consumeClick()) if (minecraft.screen == null && minecraft.player != null)
            minecraft.player.connection.sendCommand("professions");
        if(minecraft.player==null){holding=false;return;}
        String selected=SandboxClientState.selected();
        if(minecraft.screen==null&&!selected.isBlank()) {
            while(ACTION.consumeClick()) {
                if(needsForm(selected))openForm(selected);
                else send(selected,selected.equals("ledge_catch")?"start":"use");
            }
            boolean down=ACTION.isDown();
            if(selected.equals("ledge_catch")) {
                if(down&&minecraft.player.tickCount%8==0)send(selected,"hold");
                if(holding&&!down)send(selected,"cancel");
            }
            holding=down;
        } else holding=false;
    }
    private static void send(String node,String mode){
        var input=new net.minecraft.nbt.CompoundTag();input.putString("Mode",mode);
        com.siirio.jemprofessions.sandbox.SandboxProfessionRuntime.request(com.siirio.jemprofessions.progress.ProfessionId.ARCHITECT,node,"selected",input);
    }
    private static boolean needsForm(String node){
        return switch(node){
            case "long_arms_1","long_arms_2","long_arms_3","phantom_3","phantom_5","phantom_7","architect_command",
                 "crop_sense","sweep","harvest_master","herd_call","feed_herd","quick_shear","ore_sense","ore_memory","deep_resonance",
                 "wall_climb","ledge_catch","gear_swap","magic_circle","sanctuary" -> false;
            default -> true;
        };
    }
    private static void openForm(String node){
        for(var id:com.siirio.jemprofessions.progress.ProfessionId.values()){
            var skill=com.siirio.jemprofessions.tree.ProfessionCatalog.node(id,node);
            if(skill!=null){Minecraft.getInstance().setScreen(new SandboxActionScreen(null,id,skill));return;}
        }
    }
}
