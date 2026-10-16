package com.siirio.jemroles.compat.irons;

import com.siirio.jemroles.JemRoles;
import com.siirio.jemroles.race.RaceId;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class RacialSpells {
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, JemRoles.MOD_ID);
    public static final RegistryObject<AbstractSpell> LIFE_CHORUS = SPELLS.register("life_chorus", () -> new RacialSpell("life_chorus", RaceId.ANCESTORS, SchoolRegistry.HOLY_RESOURCE, 25));
    public static final RegistryObject<AbstractSpell> RIFT_AMBUSH = SPELLS.register("rift_ambush", () -> new RacialSpell("rift_ambush", RaceId.VOID_WALKERS, SchoolRegistry.ENDER_RESOURCE, 60));
    public static final RegistryObject<AbstractSpell> VERDANT_DOMINION = SPELLS.register("verdant_dominion", () -> new RacialSpell("verdant_dominion", RaceId.BORN_IN_BLOOM, SchoolRegistry.NATURE_RESOURCE, 25));
    public static final RegistryObject<AbstractSpell> SCULK_VEINS = SPELLS.register("sculk_veins", () -> new RacialSpell("sculk_veins", RaceId.INFECTED_BY_SCULK, SchoolRegistry.ELDRITCH_RESOURCE, 20));
    public static final RegistryObject<AbstractSpell> INFERNAL_ERUPTION = SPELLS.register("infernal_eruption", () -> new RacialSpell("infernal_eruption", RaceId.NETHER_HEARTEDS, SchoolRegistry.FIRE_RESOURCE, 60));
    private RacialSpells() {}
    public static void register(IEventBus bus) { SPELLS.register(bus); }
}
