package com.siirio.jemserver.smp.events;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SmpEventBlocks {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"jem_server");
    public static final RegistryObject<Block> STRUCTURE_BARRIER=BLOCKS.register("structure_barrier",StructureBarrierBlock::new);

    public static void register(IEventBus bus) { BLOCKS.register(bus); }
    private SmpEventBlocks() {}
}
