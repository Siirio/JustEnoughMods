package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRun;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityStradpole", remap = false)
public abstract class WildlifeQuestStradpoleGrowthMixin {
    @Unique private WildlifeQuestRun jem$growth;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$growth = WildlifeQuestRun.begin((Entity)(Object)this, WildlifeQuestProof.real(player), player.getItemInHand(hand));
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$growth; jem$growth = null; WildlifeQuestRun.end(run);
        if (run != null && callback.getReturnValue().consumesAction()) run.transformed(player.getItemInHand(hand), "alexsmobs:straddler", "v3_fauna_lava_nursery");
    }
}
