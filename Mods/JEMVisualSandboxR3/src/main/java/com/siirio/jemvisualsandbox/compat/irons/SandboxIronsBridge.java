package com.siirio.jemvisualsandbox.compat.irons;

import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Optional native spell projection; RaceExperience remains the gameplay owner. */
public final class SandboxIronsBridge {
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(
            SpellRegistry.SPELL_REGISTRY_KEY, "jem_visual_sandbox");
    private static final RegistryObject<AbstractSpell> LIFE_CHORUS = SPELLS.register("life_chorus",
            () -> new SandboxRacialSpell("life_chorus", 0, SchoolRegistry.HOLY_RESOURCE));
    private static final RegistryObject<AbstractSpell> RIFT_AMBUSH = SPELLS.register("rift_ambush",
            () -> new SandboxRacialSpell("rift_ambush", 1, SchoolRegistry.ENDER_RESOURCE));
    private static final RegistryObject<AbstractSpell> VERDANT_DOMINION = SPELLS.register("verdant_dominion",
            () -> new SandboxRacialSpell("verdant_dominion", 2, SchoolRegistry.NATURE_RESOURCE));
    private static final RegistryObject<AbstractSpell> SCULK_VEINS = SPELLS.register("sculk_veins",
            () -> new SandboxRacialSpell("sculk_veins", 3, SchoolRegistry.ELDRITCH_RESOURCE));
    private static final RegistryObject<AbstractSpell> INFERNAL_ERUPTION = SPELLS.register("infernal_eruption",
            () -> new SandboxRacialSpell("infernal_eruption", 4, SchoolRegistry.FIRE_RESOURCE));
    private static final String SLOT = "jem_sandbox_race";

    private SandboxIronsBridge() {}

    /** Reached reflectively only after the Iron's Spells presence guard. */
    public static void register(IEventBus modBus) {
        SPELLS.register(modBus);
        MinecraftForge.EVENT_BUS.register(SandboxIronsBridge.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.siirio.jemvisualsandbox.compat.irons.client.SandboxIronsClient.register(modBus));
    }

    public static boolean owns(ServerPlayer player) {
        return !player.server.isDedicatedServer() && player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static AbstractSpell spell(int race) {
        return switch (race) {
            case 0 -> LIFE_CHORUS.get();
            case 1 -> RIFT_AMBUSH.get();
            case 2 -> VERDANT_DOMINION.get();
            case 3 -> SCULK_VEINS.get();
            case 4 -> INFERNAL_ERUPTION.get();
            default -> throw new IllegalArgumentException("Unknown sandbox race " + race);
        };
    }

    @SubscribeEvent public static void selection(SpellSelectionManager.SpellSelectionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (owns(player) && RaceExperience.selected(player)) {
                add(event, RaceExperience.race(player), RaceExperience.tier(player));
            }
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.siirio.jemvisualsandbox.compat.irons.client.SandboxIronsClient.selection(event));
        }
    }

    public static void add(SpellSelectionManager.SpellSelectionEvent event, int race, int tier) {
        event.addSelectionOption(new SpellData(spell(race), Math.max(1, Math.min(3, tier))), SLOT, 0);
    }

    /** Native cooldowns are presentation only, including developer-mode bypass. */
    public static void sync(ServerPlayer player) {
        if (!owns(player)) return;
        var cooldowns = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        boolean changed = false;
        for (int race = 0; race < 5; race++) {
            AbstractSpell spell = spell(race);
            int remaining = RaceExperience.selected(player) && RaceExperience.race(player) == race
                    && !RaceExperience.developerMode(player)
                    ? (int)Math.min(Integer.MAX_VALUE, RaceExperience.remaining(player, "signature")) : 0;
            var current = cooldowns.getSpellCooldowns().get(spell.getSpellId());
            if (remaining == 0) changed |= cooldowns.removeCooldown(spell.getSpellId());
            else {
                int total = Math.max(remaining, RaceExperience.signatureCooldown(race));
                if (current == null || current.getSpellCooldown() != total || current.getCooldownRemaining() != remaining) {
                    cooldowns.addCooldown(spell, total, remaining);
                    changed = true;
                }
            }
        }
        if (changed) cooldowns.syncToPlayer(player);
    }
}
