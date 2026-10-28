package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.advancement.QuestSpellProof;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu$4", remap = false)
public abstract class ScrollForgeQuestMixin {
    @Unique private int jem$ink;
    @Unique private int jem$blank;
    @Unique private int jem$focus;

    @Inject(method = "m_142406_", at = @At("HEAD"))
    private void jem$beforeForge(Player player, ItemStack result, CallbackInfo callback) {
        jem$ink = jem$blank = jem$focus = 0;
        if (player.containerMenu instanceof ScrollForgeMenu menu && menu.getResultSlot() == (Object) this) {
            jem$ink = menu.getInkSlot().getItem().getCount();
            jem$blank = menu.getBlankScrollSlot().getItem().getCount();
            jem$focus = menu.getFocusSlot().getItem().getCount();
        }
    }

    @Inject(method = "m_142406_", at = @At("TAIL"))
    private void jem$afterForge(Player player, ItemStack result, CallbackInfo callback) {
        if (!(player.containerMenu instanceof ScrollForgeMenu menu) || menu.getResultSlot() != (Object) this) return;
        if (result.isEmpty()) result = ((com.justenoughmods.achievementguide.advancement.QuestResultSnapshot) menu).jem$questQuickResult();
        if (result.isEmpty()) return;
        SpellData spell = QuestSpellProof.at(result, 0);
        if (!(player instanceof ServerPlayer actor) || result.isEmpty()
                || spell.equals(SpellData.EMPTY) || menu.getInkSlot().getItem().getCount() != jem$ink - 1
                || menu.getBlankScrollSlot().getItem().getCount() != jem$blank - 1
                || menu.getFocusSlot().getItem().getCount() != jem$focus - 1) return;
        QuestActionTrigger.INSTANCE.emit(actor, "spell_forge_scroll");
        if (spell.getRarity() == SpellRarity.RARE || spell.getRarity() == SpellRarity.EPIC
                || spell.getRarity() == SpellRarity.LEGENDARY) {
            QuestActionTrigger.INSTANCE.emit(actor, "spell_forge_rare");
        }
    }
}
