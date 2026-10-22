package com.siirio.jemguisandbox;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("jem_gui_sandbox")
public final class JemGuiSandbox {
    public JemGuiSandbox() {
        com.siirio.jemguisandbox.mirror.jemcurrency.CurrencyItems.ITEMS.register(
                FMLJavaModLoadingContext.get().getModEventBus());
    }
}
