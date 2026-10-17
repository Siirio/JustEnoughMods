package com.siirio.jemprofessions.sandbox;

import com.siirio.jemprofessions.JemProfessions;
import com.siirio.jemprofessions.api.ProfessionApi;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Packaged only in the sandbox artifact; the ordinary SMP command owner is untouched. */
@Mod.EventBusSubscriber(modid = JemProfessions.MOD_ID)
public final class SandboxProfessionAccess {
    private SandboxProfessionAccess() { }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if (event.getCommandSelection() == Commands.CommandSelection.DEDICATED) return;
        event.getDispatcher().register(Commands.literal("professions")
                .requires(source -> source.getPlayer() != null && !source.getServer().isDedicatedServer())
                .executes(context -> {
                    ProfessionApi.openScreen(context.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
