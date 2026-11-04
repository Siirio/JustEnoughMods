package com.siirio.jembuilding.registry;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.construction.ConstructionPlaceholderBlock;
import com.siirio.jembuilding.item.BuilderHammerItem;
import com.siirio.jembuilding.item.BlueprintRollItem;
import com.siirio.jembuilding.item.StructureBlueprintItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BuildingContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, JEMBuilding.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JEMBuilding.MOD_ID);

    public static final RegistryObject<Block> CONSTRUCTION_PLACEHOLDER = BLOCKS.register("construction_placeholder",
            () -> new ConstructionPlaceholderBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion()
                    .instabreak().noLootTable().sound(SoundType.COPPER)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final RegistryObject<Item> BUILDER_HAMMER = ITEMS.register("builder_hammer",
            () -> new BuilderHammerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BLUEPRINT_ROLL = ITEMS.register("blueprint_roll",
            () -> new BlueprintRollItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> STRUCTURE_BLUEPRINT = ITEMS.register("structure_blueprint",
            () -> new StructureBlueprintItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(BuildingContent::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BUILDER_HAMMER);
            event.accept(BLUEPRINT_ROLL);
        }
    }

    private BuildingContent() {
    }
}
