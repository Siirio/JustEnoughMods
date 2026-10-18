package com.siirio.jemcompat.feature.transport;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TransportContent {
    private static final int AIRCRAFT_FUEL_STACK_SIZE = 16;
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JEMCompat.MOD_ID);
    public static final RegistryObject<Item> AIRCRAFT_FUEL = ITEMS.register("aircraft_fuel",
            () -> new Item(new Item.Properties().stacksTo(AIRCRAFT_FUEL_STACK_SIZE)));

    private TransportContent() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER, AircraftPolicy.SPEC, "jemcompat-aircraft-server.toml");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(TransportContent::aircraftUnloaded);
    }

    private static void aircraftUnloaded(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof AircraftFuelState state) state.jemcompat$pauseFuelClock();
    }
}
