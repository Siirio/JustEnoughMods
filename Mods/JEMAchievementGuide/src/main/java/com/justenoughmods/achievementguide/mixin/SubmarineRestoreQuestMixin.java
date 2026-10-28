package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ToolActions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.item.SubmarineEntity", remap = false)
public abstract class SubmarineRestoreQuestMixin {
    @Unique private static final String jem$repaired = "JemQuestSubmarineRepairActorV4";
    @Unique private static final String jem$scraped = "JemQuestSubmarineScrapeActorV4";
    @Unique private int jem$damageBefore;
    @Unique private int jem$oxidationBefore;
    @Unique private ItemStack jem$tool;
    @Unique private int jem$toolCount;
    @Unique private boolean jem$alreadyBoarded;
    @Unique private boolean jem$copper;
    @Unique private boolean jem$axe;
    @Shadow public abstract int getDamageLevel();
    @Shadow public abstract int getOxidizationLevel();

    @Inject(method = "m_6096_", at = @At("HEAD"))
    private void jem$beforeNativeInteraction(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$tool = null;
        if (!(player instanceof ServerPlayer)) return;
        jem$damageBefore = getDamageLevel();
        jem$oxidationBefore = getOxidizationLevel();
        jem$tool = player.getItemInHand(hand);
        jem$toolCount = jem$tool.getCount();
        jem$copper = jem$tool.is(Items.COPPER_INGOT);
        jem$axe = jem$tool.canPerformAction(ToolActions.AXE_SCRAPE);
        jem$alreadyBoarded = player.getVehicle() == (Object) this;
    }

    @Inject(method = "m_6096_", at = @At("RETURN"))
    private void jem$afterNativeInteraction(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var tool = jem$tool;
        jem$tool = null;
        if (!(player instanceof ServerPlayer actor) || actor.getAbilities().instabuild || actor.isSpectator()
                || actor instanceof net.minecraftforge.common.util.FakePlayer || tool == null
                || !callback.getReturnValue().consumesAction()) return;
        var submarine = (Entity) (Object) this;
        if (submarine.level() != actor.level() || submarine.isRemoved()) return;
        var proof = submarine.getPersistentData();
        if (jem$copper && jem$toolCount - tool.getCount() == 1
                && jem$damageBefore - getDamageLevel() == 1) proof.putUUID(jem$repaired, actor.getUUID());
        if (jem$axe && jem$oxidationBefore - getOxidizationLevel() == 1) {
            proof.putUUID(jem$scraped, actor.getUUID());
        }
        if (!jem$alreadyBoarded && actor.getVehicle() == submarine && proof.hasUUID(jem$repaired)
                && proof.hasUUID(jem$scraped) && actor.getUUID().equals(proof.getUUID(jem$repaired))
                && actor.getUUID().equals(proof.getUUID(jem$scraped))) {
            proof.remove(jem$repaired);
            proof.remove(jem$scraped);
            QuestActionTrigger.INSTANCE.emit(actor, "v3_cave_submarine_restore");
        }
    }
}
