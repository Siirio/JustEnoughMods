package com.siirio.jemworldbosstiers.enchantment;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TierEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, JemWorldBossTiers.MOD_ID);

    public static final RegistryObject<Enchantment> PROGRESSION =
            ENCHANTMENTS.register("progression", ProgressionEnchantment::new);

    private TierEnchantments() {
    }

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }
}
