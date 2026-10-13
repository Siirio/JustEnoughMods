package com.siirio.jemeconomy.registry;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.shipping.ShippingBoxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EconomyBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, JemEconomy.MOD_ID);
    public static final RegistryObject<BlockEntityType<ShippingBoxBlockEntity>> SHIPPING_BOX = BLOCK_ENTITIES.register("shipping_box",
            () -> BlockEntityType.Builder.of(ShippingBoxBlockEntity::new, EconomyBlocks.SHIPPING_BOX.get()).build(null));
    private EconomyBlockEntities() { }
}
