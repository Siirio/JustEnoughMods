package com.siirio.jemworldbosstiers.loot;

import com.mojang.serialization.Codec;
import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class TierLoot {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, JemWorldBossTiers.MOD_ID);

    static {
        MODIFIERS.register("progression_book", () -> ProgressionBookLootModifier.CODEC);
    }

    private TierLoot() {
    }

    public static void register(IEventBus bus) {
        MODIFIERS.register(bus);
    }
}
