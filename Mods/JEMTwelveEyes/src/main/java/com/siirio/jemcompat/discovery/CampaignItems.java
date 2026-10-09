package com.siirio.jemcompat.discovery;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CampaignItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JEMTwelveEyes.CONTENT_NAMESPACE);
    private static final Map<CampaignTarget, RegistryObject<Item>> LOCATORS = new LinkedHashMap<>();

    static {
        for (CampaignTarget target : CampaignTarget.values()) {
            LOCATORS.put(target, ITEMS.register("boss_locator_" + target.key(), () -> new CampaignLocatorItem(target)));
        }
    }

    private CampaignItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static RegistryObject<Item> locator(CampaignTarget target) {
        return LOCATORS.get(target);
    }

    public static Iterable<RegistryObject<Item>> locators() {
        return LOCATORS.values();
    }
}
