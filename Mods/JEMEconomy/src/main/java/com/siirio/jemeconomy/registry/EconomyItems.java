package com.siirio.jemeconomy.registry;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemeconomy.physical.PhysicalCoinItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EconomyItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JemEconomy.MOD_ID);
    public static final RegistryObject<Item> COPPER_COIN = com.siirio.jemcurrency.CurrencyItems.COPPER_COIN;
    public static final RegistryObject<Item> SILVER_COIN = com.siirio.jemcurrency.CurrencyItems.SILVER_COIN;
    public static final RegistryObject<Item> GOLD_COIN = com.siirio.jemcurrency.CurrencyItems.GOLD_COIN;
    public static final RegistryObject<Item> AETHER_COIN = com.siirio.jemcurrency.CurrencyItems.AETHER_COIN;
    public static final RegistryObject<Item> ARCANE_COIN = com.siirio.jemcurrency.CurrencyItems.ARCANE_COIN;
    public static final RegistryObject<Item> SHIPPING_BOX = ITEMS.register("shipping_box",
            () -> new com.siirio.jemeconomy.shipping.ShippingBoxItem(EconomyBlocks.SHIPPING_BOX.get(), new Item.Properties()));

    private EconomyItems() {
    }

    public static Item item(CoinDenomination denomination) {
        return switch (denomination) {
            case COPPER -> COPPER_COIN.get();
            case SILVER -> SILVER_COIN.get();
            case GOLD -> GOLD_COIN.get();
            case AETHER -> AETHER_COIN.get();
            case ARCANE -> ARCANE_COIN.get();
        };
    }

}
