package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityMimicOctopus;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityMimicOctopus", remap = false)
public abstract class WildlifeQuestMimicTrainingMixin {
    @Unique private ItemStack jem$cream;
    @Unique private boolean jem$upgraded;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$cream = player.getItemInHand(hand).copy(); jem$upgraded = ((EntityMimicOctopus)(Object)this).isUpgraded(); }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EntityMimicOctopus self = (EntityMimicOctopus)(Object)this; var actor = WildlifeQuestProof.real(player);
        if (actor != null && self.isOwnedBy(actor) && !jem$upgraded && self.isUpgraded() && WildlifeQuestProof.item(jem$cream).equals("alexsmobs:mimicream") && WildlifeQuestProof.debited(jem$cream, player.getItemInHand(hand), 1)) WildlifeQuestProof.mark(self, "mimic_trainer", actor.getUUID());
    }
    @Inject(method = "creeperExplode", at = @At("HEAD"))
    private void jem$beginBlast(CallbackInfo callback) { WildlifeQuestAbilities.begin((EntityMimicOctopus)(Object)this); }
    @Inject(method = "creeperExplode", at = @At("RETURN"))
    private void jem$endBlast(CallbackInfo callback) { WildlifeQuestAbilities.end((EntityMimicOctopus)(Object)this); }
}
