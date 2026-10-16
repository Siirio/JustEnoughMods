package com.siirio.jemroles.compat.irons.client;

import io.redspace.ironsspellbooks.player.KeyMappings;
import net.minecraft.client.KeyMapping;

/** Client-only optional integration, reached after the Iron's Spells presence guard. */
public final class RacialSpellKeys {
    private RacialSpellKeys() {}

    public static boolean sharesNativeCastKey(KeyMapping mapping) {
        return mapping.same(KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP);
    }
}
