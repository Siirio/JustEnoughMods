package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import com.justenoughmods.achievementguide.advancement.QuestSpellProof;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.ShrivingStoneItem;
import io.redspace.ironsspellbooks.item.UpgradeOrbTypeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public abstract class ArcaneAnvilQuestMixin {
    @Unique private ItemStack jem$base = ItemStack.EMPTY;
    @Unique private ItemStack jem$addition = ItemStack.EMPTY;

    @Inject(method = "m_142365_", at = @At("HEAD"))
    private void jem$beforeArcaneTake(Player player, ItemStack result, CallbackInfo callback) {
        ArcaneAnvilMenu menu = (ArcaneAnvilMenu) (Object) this;
        jem$base = menu.getSlot(0).getItem().copy();
        jem$addition = menu.getSlot(1).getItem().copy();
    }

    @Inject(method = "m_142365_", at = @At("TAIL"))
    private void jem$afterArcaneTake(Player player, ItemStack result, CallbackInfo callback) {
        ArcaneAnvilMenu menu = (ArcaneAnvilMenu) (Object) this;
        if (result.isEmpty()) result = ((com.justenoughmods.achievementguide.advancement.QuestResultSnapshot) menu).jem$questQuickResult();
        if (player instanceof ServerPlayer actor && player.containerMenu == menu && !result.isEmpty()
                && !jem$base.isEmpty() && !jem$addition.isEmpty()
                && menu.getSlot(0).getItem().getCount() == jem$base.getCount() - 1
                && menu.getSlot(1).getItem().getCount() == jem$addition.getCount() - 1) {
            var before = QuestSpellProof.spells(jem$base);
            var after = QuestSpellProof.spells(result);
            boolean levelIncreased = after.stream().anyMatch(now ->
                    before.stream().anyMatch(old -> old.getSpell() == now.getSpell()
                    && now.getLevel() > old.getLevel()));
            if (result.is(jem$base.getItem()) && levelIncreased) {
                QuestActionTrigger.INSTANCE.emit(actor, "spell_arcane_upgrade");
            }
            if (result.is(jem$base.getItem()) && jem$addition.getItem() instanceof Scroll
                    && Utils.canImbue(jem$base) && after.stream().anyMatch(now ->
                    before.stream().noneMatch(old -> old.getSpell() == now.getSpell()))) {
                QuestActionTrigger.INSTANCE.emit(actor, "spell_weapon_imbue");
            }
            if (jem$addition.getItem() instanceof ShrivingStoneItem && !before.isEmpty()
                    && after.size() < before.size()) {
                QuestActionTrigger.INSTANCE.emit(actor, "spell_shriving_remove");
            }
            if (result.is(jem$base.getItem()) && UpgradeOrbTypeData.has(jem$addition)
                    && UpgradeData.getUpgradeData(result).getTotalUpgrades()
                    > UpgradeData.getUpgradeData(jem$base).getTotalUpgrades()) {
                QuestActionTrigger.INSTANCE.emit(actor, "spell_apply_upgrade_orb");
            }
        }
        jem$base = ItemStack.EMPTY;
        jem$addition = ItemStack.EMPTY;
    }
}
