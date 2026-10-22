package com.siirio.jemguisandbox;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="jem_gui_sandbox",value=Dist.CLIENT)
public final class ClientHooks {
    private static final KeyMapping OPEN=new KeyMapping("key.jem_gui_sandbox.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,"key.categories.jem_gui_sandbox");
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        var dispatcher=event.getDispatcher();
        dispatcher.register(net.minecraft.commands.Commands.literal("jemgui")
            .executes(c->schedule("hub"))
            .then(net.minecraft.commands.Commands.argument("screen",StringArgumentType.word())
                .suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("smp","settlements","death","profile","players","parties","market","shops","prizes","encounter","result","defeat","founding","settlement-directory","settlement-guide","bosses","landmarks","homes","reset"),b))
                .executes(c->schedule(StringArgumentType.getString(c,"screen")))));
        for(String route:java.util.List.of("smp","settlements","death"))
            dispatcher.register(net.minecraft.commands.Commands.literal(route).executes(c->schedule(route)));
    }
    private static int schedule(String route) {Demo.defer(()->Demo.open(route));return 1;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase==TickEvent.Phase.END) {
            while(OPEN.consumeClick())Demo.open("hub");
            Demo.drainReplies();
        }
    }
    @SubscribeEvent public static void screenKey(ScreenEvent.KeyPressed.Pre event) {
        if(OPEN.matches(event.getKeyCode(),event.getScanCode())) {
            event.setCanceled(true);Demo.open("hub");
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {Demo.reset();}
    @Mod.EventBusSubscriber(modid="jem_gui_sandbox",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {event.register(OPEN);}
    }
}
