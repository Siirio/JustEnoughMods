package com.siirio.jemguisandbox.mirror.jemcurrency;

import com.siirio.jemguisandbox.mirror.jemeconomy.physical.CoinDenomination;
import java.util.EnumMap;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;

/** Display-only coin faces, under the sandbox namespace; no currency capabilities. */
public final class CurrencyItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "jem_gui_sandbox");
    private static final EnumMap<CoinDenomination, RegistryObject<Item>> COINS = new EnumMap<>(CoinDenomination.class);
    static {
        for (var denomination : CoinDenomination.values())
            COINS.put(denomination, ITEMS.register(denomination.id(), () -> new Item(new Item.Properties())));
    }
    public static Item item(CoinDenomination denomination) { return COINS.get(denomination).get(); }
}
