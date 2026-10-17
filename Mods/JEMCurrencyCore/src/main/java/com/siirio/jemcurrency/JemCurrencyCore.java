package com.siirio.jemcurrency;

import com.siirio.jemeconomy.merchant.CoinMerchantRuntime;
import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Single runtime owner of legacy Coins, ledger and funded native merchant transactions. */
@Mod(JemCurrencyCore.MOD_ID)
public final class JemCurrencyCore {
    public static final String MOD_ID = "jem_currency_core";
    public static final String LEGACY_NAMESPACE = "jem_economy";

    public JemCurrencyCore() {
        CurrencyItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        EconomyNetwork.register();
        MinecraftForge.EVENT_BUS.register(EconomyNetwork.class);
        MinecraftForge.EVENT_BUS.register(ItemPolicyRegistry.class);
        MinecraftForge.EVENT_BUS.register(CoinMerchantRuntime.class);
    }
}
