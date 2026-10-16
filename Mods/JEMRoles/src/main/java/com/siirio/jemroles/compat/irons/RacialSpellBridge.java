package com.siirio.jemroles.compat.irons;

import com.siirio.jemroles.client.RoleClientState;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceService;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Adds the chosen race spell to Iron's native selection/HUD without a physical book or scroll. */
public final class RacialSpellBridge {
    private static final String SLOT = "jem_race";
    private RacialSpellBridge() {}
    @SubscribeEvent public static void selection(SpellSelectionManager.SpellSelectionEvent event) {
        RaceId race = event.getEntity() instanceof ServerPlayer player
                ? RaceService.snapshot(player).map(value -> value.race()).orElse(null)
                : RoleClientState.race();
        if (race == null) return;
        int tier = event.getEntity() instanceof ServerPlayer player
                ? RaceService.snapshot(player).map(value -> value.spellTier()).orElse(1)
                : RoleClientState.spellTier();
        event.addSelectionOption(new SpellData(spell(race), Math.max(1, Math.min(3, tier))), SLOT, 0);
    }
    public static AbstractSpell spell(RaceId race) { return switch (race) {
        case ANCESTORS -> RacialSpells.LIFE_CHORUS.get(); case VOID_WALKERS -> RacialSpells.RIFT_AMBUSH.get();
        case BORN_IN_BLOOM -> RacialSpells.VERDANT_DOMINION.get(); case INFECTED_BY_SCULK -> RacialSpells.SCULK_VEINS.get();
        case NETHER_HEARTEDS -> RacialSpells.INFERNAL_ERUPTION.get();
    }; }
    /** Iron's cooldown capability is a presentation projection of the persisted race owner. */
    public static void syncCooldown(ServerPlayer player) {
        if (!player.server.isDedicatedServer()) return;
        var record = RaceService.record(player).orElse(null);
        var cooldowns = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        boolean changed = false;
        for (RaceId race : RaceId.values()) {
            AbstractSpell spell = spell(race);
            int remaining = record != null && record.race() == race
                    ? (int)Math.min(Integer.MAX_VALUE, record.remaining("signature", player.level().getGameTime())) : 0;
            var current = cooldowns.getSpellCooldowns().get(spell.getSpellId());
            if (remaining == 0) changed |= cooldowns.removeCooldown(spell.getSpellId());
            else {
                int total = Math.max(remaining, com.siirio.jemroles.trait.RaceAbilities.signatureCooldown(race));
                if (current == null || current.getSpellCooldown() != total || current.getCooldownRemaining() != remaining) {
                    cooldowns.addCooldown(spell, total, remaining);
                    changed = true;
                }
            }
        }
        if (changed) cooldowns.syncToPlayer(player);
    }
    public static void refreshClient(boolean select) {
        io.redspace.ironsspellbooks.player.ClientMagicData.updateSpellSelectionManager();
        if (!select || RoleClientState.race() == null) return;
        var manager = io.redspace.ironsspellbooks.player.ClientMagicData.getSpellSelectionManager();
        var desired = spell(RoleClientState.race());
        for (int i = 0; i < manager.getSpellCount(); i++) if (manager.getSpellData(i).getSpell().equals(desired)) { manager.makeSelection(i); break; }
    }

}
