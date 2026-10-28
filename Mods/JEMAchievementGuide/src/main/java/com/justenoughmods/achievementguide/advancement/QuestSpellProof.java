package com.justenoughmods.achievementguide.advancement;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Read-only adapters for Iron's nullable native spell-container API */
public final class QuestSpellProof {
    private QuestSpellProof() {}

    public static List<SpellSlot> spells(ItemStack stack) {
        ISpellContainer container = stack.isEmpty() ? null : ISpellContainer.get(stack);
        return container == null ? List.of() : container.getActiveSpells();
    }

    public static SpellData at(ItemStack stack, int index) {
        ISpellContainer container = stack.isEmpty() ? null : ISpellContainer.get(stack);
        return container == null || index < 0 || index >= container.getMaxSpellCount()
                ? SpellData.EMPTY : container.getSpellAtIndex(index);
    }
}
