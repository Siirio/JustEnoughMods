package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recognized native string/frame recipe, actual paid output take and conserved bow data. */
@Mixin(AnvilMenu.class)
public abstract class SimplyBowAnvilQuestMixin {
    @Unique private ItemStack jem$first = ItemStack.EMPTY;
    @Unique private ItemStack jem$second = ItemStack.EMPTY;
    @Unique private int jem$levels;
    @Unique private int jem$cost;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$beforeUpgrade(Player player, ItemStack result, CallbackInfo callback) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        jem$first = menu.getSlot(0).getItem().copy();
        jem$second = menu.getSlot(1).getItem().copy();
        jem$levels = player.experienceLevel;
        jem$cost = menu.getCost();
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void jem$upgradeTaken(Player player, ItemStack result, CallbackInfo callback) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack first = jem$first, second = jem$second;
        jem$first = jem$second = ItemStack.EMPTY;
        if (!(player instanceof ServerPlayer actor) || actor.containerMenu != menu || jem$cost <= 0
                || jem$levels - actor.experienceLevel != jem$cost) return;
        ItemStack bow = first.getItem() instanceof SimplyBowItem ? first : second;
        ItemStack componentStack = first.getItem() instanceof SimplyBowItem ? second : first;
        if (!(bow.getItem() instanceof SimplyBowItem) || bow.getCount() != 1
                || !(componentStack.getItem() instanceof BowUpgradeComponentItem component)) return;
        boolean bowFirst = bow == first;
        if (!menu.getSlot(0).getItem().isEmpty()) return;
        ItemStack remaining = menu.getSlot(1).getItem();
        if (bowFirst ? componentStack.getCount() - remaining.getCount() != 1
                || !remaining.isEmpty() && !ItemStack.isSameItemSameTags(componentStack, remaining) : !remaining.isEmpty()) return;
        BowUpgradeData before = BowUpgradeData.from(bow), upgraded = component.applyTo(before);
        boolean string = upgraded.stringLevel() == before.stringLevel() + 1 && upgraded.frameLevel() == before.frameLevel();
        boolean frame = upgraded.frameLevel() == before.frameLevel() + 1 && upgraded.stringLevel() == before.stringLevel();
        if (!(string || frame) || upgraded.runeEtching() != before.runeEtching()
                || component.getAnvilCost(before, upgraded) != jem$cost) return;
        ItemStack taken = result.isEmpty() ? ((QuestResultSnapshot) menu).jem$questQuickResult() : result;
        ItemStack expected = bow.copy();
        upgraded.write(expected);
        if (!taken.isEmpty() && taken.getCount() == 1 && ItemStack.isSameItemSameTags(taken, expected)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_bow_workbench");
        }
    }
}
