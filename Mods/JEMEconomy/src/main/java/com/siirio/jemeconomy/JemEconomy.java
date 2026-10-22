package com.siirio.jemeconomy;

import com.siirio.jemeconomy.full.market.MarketRuntime;
import com.siirio.jemeconomy.merchant.CoinMerchantRuntime;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemeconomy.registry.EconomyBlockEntities;
import com.siirio.jemeconomy.registry.EconomyBlocks;
import com.siirio.jemeconomy.registry.EconomyItems;
import com.siirio.jemeconomy.registry.EconomyMenus;
import com.siirio.jemeconomy.network.EconomyNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
        modBus.addListener(JemEconomy::addCreativeItems);

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                com.siirio.jemeconomy.config.EconomyConfig.SPEC);
        modBus.addListener(JemEconomy::configReloaded);
        com.siirio.jemcurrency.CurrencyItems.ITEMS.register(modBus);
        EconomyNetwork.register();
        MinecraftForge.EVENT_BUS.register(EconomyNetwork.class);
        MinecraftForge.EVENT_BUS.register(ItemPolicyRegistry.class);
        MinecraftForge.EVENT_BUS.register(com.siirio.jemeconomy.policy.EconomyRecipePolicy.class);
        MinecraftForge.EVENT_BUS.register(CoinMerchantRuntime.class);
        MinecraftForge.EVENT_BUS.register(com.siirio.jemeconomy.physical.CoinMigration.class);
        MinecraftForge.EVENT_BUS.register(MarketRuntime.class);
        MinecraftForge.EVENT_BUS.register(com.siirio.jemeconomy.full.audit.EconomyRuntimeExport.class);
    }

    private static void configReloaded(net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != com.siirio.jemeconomy.config.EconomyConfig.SPEC) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.execute(() -> {
            com.siirio.jemeconomy.policy.EconomyRecipePolicy.reconcile(server);
            boolean enabled = com.siirio.jemeconomy.api.EconomyApi.isEnabled(server);
            for (var player : server.getPlayerList().getPlayers()) {
                if (!enabled && (player.containerMenu instanceof com.siirio.jemeconomy.api.CurrencyContainerAccess
                        || player.containerMenu instanceof com.siirio.jemeconomy.merchant.NativeMerchantAccess access && access.jem$session() != null))
                    player.closeContainer();
                EconomyNetwork.sendFeature(player);
            }
        });
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(EconomyItems.SHIPPING_BOX);
        }
    }
}
