package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.item.unique.SimplyBowItem", remap = false)
public abstract class CombatRuneIceVolleyQuestMixin {
    @Inject(method = "shootFan", at = @At("HEAD"))
    private void jem$begin(SimplyBowItem bow, ServerLevel level, LivingEntity actor, InteractionHand hand, ItemStack stack, List<ItemStack> ammunition, float speed, float divergence, boolean critical, LivingEntity target, int quantity, CallbackInfo callback) {
        BowUpgradeData data = BowUpgradeData.from(stack);
        boolean changed = (Object)this instanceof CombatIceRuneQuantityQuestAccess access && data.runeEtching() == RuneEtching.BOUNTY && quantity > access.jem$nativeQuantity(data.withRune(RuneEtching.NONE));
        if (changed) CombatRuneProof.deliveredBow(actor, stack, "bounty"); else CombatRuneProof.branch("");
    }
    @Inject(method = "shootFan", at = @At("RETURN"))
    private void jem$end(SimplyBowItem bow, ServerLevel level, LivingEntity actor, InteractionHand hand, ItemStack stack, List<ItemStack> ammunition, float speed, float divergence, boolean critical, LivingEntity target, int quantity, CallbackInfo callback) { CombatRuneProof.leave(); }
}
