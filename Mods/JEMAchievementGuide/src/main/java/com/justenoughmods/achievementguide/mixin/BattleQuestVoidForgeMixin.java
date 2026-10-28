package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.item.context.UseOnContext; import net.minecraft.world.InteractionResult;
@Mixin(value = com.github.L_Ender.cataclysm.items.Void_forge.class, remap = false)
public abstract class BattleQuestVoidForgeMixin {
    @Inject(method="m_6225_", at=@At("HEAD"))
    private void jem$begin(UseOnContext context, CallbackInfoReturnable<InteractionResult> ci) { BattleQuestArtifacts.begin(context.getPlayer(), context.getItemInHand(), "earth"); }
    @Inject(method="m_6225_", at=@At("RETURN"))
    private void jem$end(UseOnContext context, CallbackInfoReturnable<InteractionResult> ci) { BattleQuestArtifacts.end(); }
}
