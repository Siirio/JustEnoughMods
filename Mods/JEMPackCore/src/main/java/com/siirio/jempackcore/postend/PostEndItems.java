package com.siirio.jempackcore.postend;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PostEndItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JEMTwelveEyes.CONTENT_NAMESPACE);
    public static final RegistryObject<Item> MYSTERY_LOCATOR = ITEMS.register("mystery_locator", () -> new PostEndLocatorItem(PostEndTarget.OBLITERATOR, true));
    public static final RegistryObject<Item> ENDER_GUARDIAN_LOCATOR = ITEMS.register("post_end_locator_ender_guardian", () -> new PostEndLocatorItem(PostEndTarget.ENDER_GUARDIAN, false));
    public static final RegistryObject<Item> ENDERSENT_LOCATOR = ITEMS.register("post_end_locator_endersent", () -> new PostEndLocatorItem(PostEndTarget.ENDERSENT, false));
    public static final RegistryObject<Item> SHULKER_MIMIC_LOCATOR = ITEMS.register("post_end_locator_shulker_mimic", () -> new PostEndLocatorItem(PostEndTarget.SHULKER_MIMIC, false));

    private PostEndItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(PostEndItems::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MYSTERY_LOCATOR.get());
            event.accept(ENDER_GUARDIAN_LOCATOR.get());
            event.accept(ENDERSENT_LOCATOR.get());
            event.accept(SHULKER_MIMIC_LOCATOR.get());
        }
    }
}
