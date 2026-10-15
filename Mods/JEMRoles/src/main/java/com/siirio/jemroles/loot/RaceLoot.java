package com.siirio.jemroles.loot;

import com.mojang.serialization.Codec;
import com.siirio.jemroles.JemRoles;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class RaceLoot {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, JemRoles.MOD_ID);
    static { MODIFIERS.register("piglin_fortune", () -> PiglinFortuneLoot.CODEC); }
    private RaceLoot() {}
    public static void register(IEventBus bus) { MODIFIERS.register(bus); }
}
