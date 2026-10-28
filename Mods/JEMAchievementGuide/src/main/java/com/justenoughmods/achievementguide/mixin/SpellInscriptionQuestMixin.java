package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.advancement.QuestSpellProof;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InscriptionTableMenu.class, remap = false)
public abstract class SpellInscriptionQuestMixin {
    @Shadow @Final private Player player;
    @Shadow public abstract Slot getSpellBookSlot();
    @Shadow public abstract Slot getScrollSlot();
    @Unique private SpellData jem$oldSpell = SpellData.EMPTY;
    @Unique private SpellData jem$chosenSpell = SpellData.EMPTY;
    @Unique private int jem$scrollCount;

    @Inject(method = "doInscription", at = @At("HEAD"))
    private void jem$beforeInscription(int index, CallbackInfo callback) {
        jem$oldSpell = QuestSpellProof.at(getSpellBookSlot().getItem(), index);
        jem$chosenSpell = QuestSpellProof.at(getScrollSlot().getItem(), 0);
        jem$scrollCount = getScrollSlot().getItem().getCount();
    }

    @Inject(method = "doInscription", at = @At("TAIL"))
    private void jem$afterInscription(int index, CallbackInfo callback) {
        if (player instanceof ServerPlayer actor && player.containerMenu == (Object) this
                && !jem$chosenSpell.equals(SpellData.EMPTY) && !jem$oldSpell.equals(jem$chosenSpell)
                && getScrollSlot().getItem().getCount() == jem$scrollCount - 1
                && QuestSpellProof.at(getSpellBookSlot().getItem(), index).equals(jem$chosenSpell)) {
            QuestActionTrigger.INSTANCE.emit(actor, "spell_inscribe_book");
        }
        jem$oldSpell = jem$chosenSpell = SpellData.EMPTY;
    }
}
