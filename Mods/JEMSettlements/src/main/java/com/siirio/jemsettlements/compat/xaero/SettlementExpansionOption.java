package com.siirio.jemsettlements.compat.xaero;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.map.gui.GuiMap;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

/** Exactly one native action; the scoped dropdown adapter supplies its two-line cell. */
public final class SettlementExpansionOption extends RightClickOption {
    public final SettlementMapClient.Draft draft;
    public SettlementExpansionOption(SettlementMapClient.Draft draft, GuiMap map) {
        super("gui.jem_settlements.buy_for",0,map); this.draft=draft;
    }
    @Override public Component getDisplayName() {
        return Component.translatable("gui.jem_settlements.buy_for").append("                 ").append(SettlementMapClient.price(draft));
    }
    @Override public void onAction(Screen screen) { SettlementMapClient.confirm(draft); }
}
