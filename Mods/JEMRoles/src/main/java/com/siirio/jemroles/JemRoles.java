package com.siirio.jemroles;

import com.siirio.jemroles.command.RoleCommands;
import com.siirio.jemroles.trait.RaceTraitRuntime;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod(JemRoles.MOD_ID)
public final class JemRoles {
    public static final String MOD_ID = "jem_roles";

    public JemRoles() {
        MinecraftForge.EVENT_BUS.register(RoleCommands.class);
        MinecraftForge.EVENT_BUS.register(RaceTraitRuntime.class);
        if (ModList.get().isLoaded("irons_spellbooks")) {
            MinecraftForge.EVENT_BUS.register(com.siirio.jemroles.compat.irons.IronsRaceBridge.class);
        }
    }
}
