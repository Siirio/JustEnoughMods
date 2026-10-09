package com.siirio.jemcompat.gate;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CampaignBlocks {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,JEMTwelveEyes.MOD_ID);
    public static final RegistryObject<Block> LOCKED_BOSS_BARRIER=BLOCKS.register("locked_boss_barrier",CampaignBarrierBlock::new);

    public static void register(IEventBus bus) { BLOCKS.register(bus); }
    private CampaignBlocks() {}
}
