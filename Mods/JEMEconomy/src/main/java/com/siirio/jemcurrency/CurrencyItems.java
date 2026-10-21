package com.siirio.jemcurrency;


import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemeconomy.physical.PhysicalCoinItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CurrencyItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JemCurrencyCore.LEGACY_NAMESPACE);
    public static final RegistryObject<Item> COPPER_COIN = register(CoinDenomination.COPPER);
    public static final RegistryObject<Item> SILVER_COIN = register(CoinDenomination.SILVER);
    public static final RegistryObject<Item> GOLD_COIN = register(CoinDenomination.GOLD);
    public static final RegistryObject<Item> AETHER_COIN = register(CoinDenomination.AETHER);
    public static final RegistryObject<Item> ARCANE_COIN = register(CoinDenomination.ARCANE);
    private CurrencyItems() {
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

    private static RegistryObject<Item> register(CoinDenomination denomination) {
        return ITEMS.register(denomination.id(), () -> new PhysicalCoinItem(denomination));
    }
}
