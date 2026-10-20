package com.siirio.jemeconomy.registry;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.shipping.ShippingBoxBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EconomyBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, JemEconomy.MOD_ID);
    public static final RegistryObject<Block> SHIPPING_BOX = BLOCKS.register("shipping_box", () -> new ShippingBoxBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 3.0F).sound(SoundType.WOOD).noOcclusion()));
    private EconomyBlocks() { }
}
