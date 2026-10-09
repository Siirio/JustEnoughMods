package com.siirio.jemcompat.feature.transport;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TransportContent {
    private static final int AIRCRAFT_FUEL_STACK_SIZE = 32;
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JEMCompat.MOD_ID);
    public static final RegistryObject<Item> AIRCRAFT_FUEL = ITEMS.register("aircraft_fuel",
            () -> new Item(new Item.Properties().stacksTo(AIRCRAFT_FUEL_STACK_SIZE)));

    private TransportContent() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
