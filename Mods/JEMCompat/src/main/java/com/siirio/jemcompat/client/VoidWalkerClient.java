package com.siirio.jemcompat.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemcompat.JEMCompat;
import com.siirio.jemcompat.feature.kaleidoscope.VoidWalkerNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=JEMCompat.MOD_ID,value=Dist.CLIENT)
public final class VoidWalkerClient {
    private static final String DISABLED="JemCompatVoidWalkerDisabled";
    private static final KeyMapping TOGGLE=new KeyMapping("key.jemcompat.void_walker_toggle",
            InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F7,"key.categories.jemcompat");
    private VoidWalkerClient() { }

    @Mod.EventBusSubscriber(modid=JEMCompat.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
            if(ModList.get().isLoaded("kaleidoscope_end")) event.register(TOGGLE);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft client=Minecraft.getInstance();
        while(TOGGLE.consumeClick()) if(client.screen==null&&client.getConnection()!=null
                &&VoidWalkerNetwork.supported(client.getConnection().getConnection())) VoidWalkerNetwork.toggle();
    }

    public static void accept(VoidWalkerNetwork.Status status) {
        var player=Minecraft.getInstance().player;
        if(player==null) return;
        if(status.enabled()) player.getPersistentData().remove(DISABLED);
        else player.getPersistentData().putBoolean(DISABLED,true);
        if(status.announce()) player.displayClientMessage(Component.translatable(status.equipped()
                ? status.enabled()?"message.jemcompat.void_walker.enabled":"message.jemcompat.void_walker.disabled"
                : "message.jemcompat.void_walker.requires_enchantment"),true);
    }
}
