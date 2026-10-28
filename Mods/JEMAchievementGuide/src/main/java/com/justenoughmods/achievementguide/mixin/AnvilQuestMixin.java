package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilQuestMixin {
    @Unique private ItemStack jem$base = ItemStack.EMPTY;
    @Unique private ItemStack jem$addition = ItemStack.EMPTY;
    @Unique private int jem$paidLevels;
    @Unique private int jem$cost;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$beforeTake(Player player, ItemStack result, CallbackInfo callback) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        jem$base = menu.getSlot(0).getItem().copy();
        jem$addition = menu.getSlot(1).getItem().copy();
        jem$paidLevels = player.experienceLevel;
        jem$cost = menu.getCost();
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void jem$afterTake(Player player, ItemStack result, CallbackInfo callback) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        if (result.isEmpty()) result = ((com.justenoughmods.achievementguide.advancement.QuestResultSnapshot) menu).jem$questQuickResult();
        if (!(player instanceof ServerPlayer actor) || player.containerMenu != menu || result.isEmpty()
                || jem$cost <= 0 || jem$paidLevels - player.experienceLevel != jem$cost) return;
        if (menu.getSlot(0).getItem().isEmpty() && result.is(jem$base.getItem()) && result.isDamageableItem()
                && jem$toolOrWeapon(result)
                && result.getDamageValue() < jem$base.getDamageValue()) {
            QuestActionTrigger.INSTANCE.emit(actor, "anvil_repair_result");
        }
        CompoundTag decorations = result.getTagElement("HelmetDecorations");
        ItemStack helmetBefore = jem$base.is(Items.GOAT_HORN) ? jem$addition : jem$base;
        boolean hornConsumed = jem$addition.is(Items.GOAT_HORN)
                ? jem$addition.getCount() - menu.getSlot(1).getItem().getCount() == 1
                : jem$base.is(Items.GOAT_HORN) && jem$base.getCount() - menu.getSlot(0).getItem().getCount() == 1;
        if (result.getItem() instanceof ArmorItem armor && armor.getType() == ArmorItem.Type.HELMET
                && result.is(helmetBefore.getItem()) && hornConsumed
                && decorations != null && ("primal:goat_horn".equals(decorations.getString("left"))
                || "primal:goat_horn".equals(decorations.getString("right")))
                && !java.util.Objects.equals(helmetBefore.getTagElement("HelmetDecorations"), decorations)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_fauna_horned_helmet");
        }
        jem$base = ItemStack.EMPTY;
        jem$addition = ItemStack.EMPTY;
    }

    @Unique private static boolean jem$toolOrWeapon(ItemStack stack) {
        var item = stack.getItem();
        return stack.is(net.minecraftforge.common.Tags.Items.TOOLS)
                || item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.ProjectileWeaponItem
                || item instanceof net.minecraft.world.item.TridentItem
                || item instanceof net.minecraft.world.item.FishingRodItem
                || item instanceof net.minecraft.world.item.ShearsItem
                || item instanceof net.minecraft.world.item.ShieldItem
                || item instanceof net.minecraft.world.item.FlintAndSteelItem
                || item instanceof net.minecraft.world.item.BrushItem;
    }
}
