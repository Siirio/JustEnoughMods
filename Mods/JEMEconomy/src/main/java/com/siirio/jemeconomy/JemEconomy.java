package com.siirio.jemeconomy;

import com.siirio.jemeconomy.command.CoinCommands;
import com.siirio.jemeconomy.market.MarketRuntime;
import com.siirio.jemeconomy.merchant.CoinMerchantRuntime;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemeconomy.registry.EconomyBlockEntities;
import com.siirio.jemeconomy.registry.EconomyBlocks;
import com.siirio.jemeconomy.registry.EconomyItems;
import com.siirio.jemeconomy.registry.EconomyMenus;
import com.siirio.jemeconomy.network.EconomyNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(JemEconomy.MOD_ID)
public final class JemEconomy {
    public static final String MOD_ID = "jem_economy";

    public JemEconomy() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EconomyBlocks.BLOCKS.register(modBus);
        EconomyItems.ITEMS.register(modBus);
        EconomyBlockEntities.BLOCK_ENTITIES.register(modBus);
        EconomyMenus.MENUS.register(modBus);
        EconomyNetwork.register();
        MinecraftForge.EVENT_BUS.register(CoinCommands.class);
        MinecraftForge.EVENT_BUS.register(EconomyNetwork.class);
        MinecraftForge.EVENT_BUS.register(ItemPolicyRegistry.class);
        MinecraftForge.EVENT_BUS.register(MarketRuntime.class);
        MinecraftForge.EVENT_BUS.register(CoinMerchantRuntime.class);
    }
}
