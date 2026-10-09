package com.siirio.jempackcore.enchantment;

import com.siirio.jempackcore.JEMPackCore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, JEMPackCore.MOD_ID);
    public static final RegistryObject<Enchantment> EXCAVATION = ENCHANTMENTS.register("excavation", ExcavationEnchantment::new);

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }

    private CoreEnchantments() {
    }
}
