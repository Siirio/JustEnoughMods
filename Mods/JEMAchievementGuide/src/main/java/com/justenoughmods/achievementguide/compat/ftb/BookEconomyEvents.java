package com.justenoughmods.achievementguide.compat.ftb;

import com.siirio.jemeconomy.api.EconomyOperationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Isolated optional Economy adapter; loaded only when the domain is present. */
public final class BookEconomyEvents {
    private BookEconomyEvents() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookEconomyEvents.class); }
    @SubscribeEvent public static void completed(EconomyOperationEvent event) {
        BookRules.operation(event.server(), event.player(), event.kind().name(), event.item());
    }
}
