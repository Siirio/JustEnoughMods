package com.siirio.mobhome.feature.shelter;

import com.siirio.mobhome.MobHome;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Ordinary companion gameplay; deliberately independent of dedicated-only JEM Events. */
public final class ShelterContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MobHome.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MobHome.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MobHome.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MobHome.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MobHome.MOD_ID);
    public static final RegistryObject<RecipeSerializer<ShelterRecipe>> RECIPE = RECIPES.register("matching_wood_shelter", ShelterRecipe.Serializer::new);
    public static final RegistryObject<Block> SMALL = BLOCKS.register("small_animal_home", () -> block(2));
    public static final RegistryObject<Block> LARGE = BLOCKS.register("large_animal_home", () -> block(3));
    public static final RegistryObject<Item> SMALL_ITEM = ITEMS.register("small_animal_home", () -> new BlockItem(SMALL.get(), new Item.Properties()));
    public static final RegistryObject<Item> LARGE_ITEM = ITEMS.register("large_animal_home", () -> new BlockItem(LARGE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<ShelterController>> CONTROLLER = ENTITIES.register("animal_home",
            () -> BlockEntityType.Builder.of(ShelterController::new, SMALL.get(), LARGE.get()).build(null));
    public static final RegistryObject<MenuType<ShelterMenu>> MENU = MENUS.register("animal_home", () -> IForgeMenuType.create(ShelterMenu::new));

    private ShelterContent() { }
    private static Block block(int size) {
        return new ShelterBlock(size, BlockBehaviour.Properties.of().strength(2.5F, 3.0F).sound(SoundType.WOOD).noOcclusion());
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); RECIPES.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ShelterEvents.class);
    }
}
