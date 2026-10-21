package com.siirio.jemvisualsandbox.compat.irons.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemvisualsandbox.compat.irons.SandboxIronsBridge;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.KeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client-only optional bridge: native HUD, native wheel and native cast owner. */
public final class SandboxIronsClient {
    private static final KeyMapping NEXT = new KeyMapping("key.jem_sandbox.next_spell", InputConstants.Type.KEYSYM,
            InputConstants.KEY_TAB, "key.categories.jem_sandbox");
    private static boolean selected;
    private static int race, tier = 1;

    private SandboxIronsClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(SandboxIronsClient::keys);
        MinecraftForge.EVENT_BUS.addListener(SandboxIronsClient::tick);
    }

    private static void keys(RegisterKeyMappingsEvent event) { event.register(NEXT); }

    public static void accept(boolean chosen, int chosenRace, int spellTier) {
        selected = chosen;
        race = chosenRace;
        tier = Math.max(1, Math.min(3, spellTier));
    }

    public static boolean shouldUseNativeCast() {
        var client = Minecraft.getInstance();
        return selected && client.player != null && (com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported());
    }

    public static boolean usesNativeKey(KeyMapping mapping) {
        return shouldUseNativeCast() && mapping.same(KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP);
    }

    public static boolean nativeCastOwnsKey(KeyMapping mapping) { return usesNativeKey(mapping); }

    public static void selection(SpellSelectionManager.SpellSelectionEvent event) {
        var client = Minecraft.getInstance();
        if (shouldUseNativeCast() && event.getEntity() == client.player) SandboxIronsBridge.add(event, race, tier);
    }

    public static void refresh(boolean select) {
        var client = Minecraft.getInstance();
        if (client.player == null || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported())) return;
        ClientMagicData.updateSpellSelectionManager();
        if (!select || !selected) return;
        var manager = ClientMagicData.getSpellSelectionManager();
        var desired = SandboxIronsBridge.spell(race);
        for (int i = 0; i < manager.getSpellCount(); i++) {
            if (manager.getSpellData(i).getSpell().equals(desired)) {
                manager.makeSelection(i);
                break;
            }
        }
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (NEXT.consumeClick()) {
            if (!shouldUseNativeCast() || Minecraft.getInstance().screen != null) continue;
            // Respect a user's native Iron wheel binding if they assigned Tab.
            if (NEXT.same(KeyMappings.SPELL_WHEEL_KEYMAP) || NEXT.same(KeyMappings.SPELL_WHEEL_TOGGLE_KEYMAP)) continue;
            var manager = ClientMagicData.getSpellSelectionManager();
            int count = manager.getSpellCount();
            if (count > 1) manager.makeSelection((manager.getSelectionIndex() + 1) % count);
        }
    }
}
