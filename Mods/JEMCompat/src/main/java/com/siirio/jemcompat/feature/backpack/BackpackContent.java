package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.backpacked.core.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

public final class BackpackContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "jemcompat");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "jemcompat");
    // Retired block/item IDs remain readable in existing worlds; no recipe, creative entry or station UI.
    public static final RegistryObject<Block> AUGMENT_BAY = BLOCKS.register("augment_bay", () -> new AugmentBayBlock(
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> AUGMENT_BAY_ITEM = ITEMS.register("augment_bay", () -> new BlockItem(AUGMENT_BAY.get(), new Item.Properties()));

    private BackpackContent() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(BackpackContent::registerAugment);
    }

    private static void registerAugment(RegisterEvent event) {
        event.register(ModRegistries.AUGMENT_TYPES.getKey(), helper -> helper.register(WorkbenchAugment.TYPE.id(), WorkbenchAugment.TYPE));
    }

}
