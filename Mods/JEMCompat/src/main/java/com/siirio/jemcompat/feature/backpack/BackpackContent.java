package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.backpacked.core.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

public final class BackpackContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "jemcompat");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "jemcompat");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "jemcompat");
    public static final RegistryObject<Block> AUGMENT_BAY = BLOCKS.register("augment_bay", () -> new AugmentBayBlock(
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> AUGMENT_BAY_ITEM = ITEMS.register("augment_bay", () -> new BlockItem(AUGMENT_BAY.get(), new Item.Properties()));
    public static final RegistryObject<MenuType<AugmentBayMenu>> AUGMENT_BAY_MENU = MENUS.register("augment_bay",
            () -> IForgeMenuType.create((id, inventory, data) -> new AugmentBayMenu(id, inventory, data.readBlockPos())));

    private BackpackContent() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        bus.addListener(BackpackContent::registerAugment);
        bus.addListener(BackpackContent::creativeContents);
    }

    private static void registerAugment(RegisterEvent event) {
        event.register(ModRegistries.AUGMENT_TYPES.getKey(), helper -> helper.register(WorkbenchAugment.TYPE.id(), WorkbenchAugment.TYPE));
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(AUGMENT_BAY_ITEM);
    }
}
