package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level; import net.minecraft.world.entity.LivingEntity; import net.minecraft.world.entity.player.Player; import net.minecraft.world.InteractionHand; import net.minecraft.world.InteractionResultHolder;
@Mixin(value = com.github.L_Ender.cataclysm.items.Sandstorm_In_A_Bottle.class, remap = false)
public abstract class BattleQuestSandBottleMixin {
    @Inject(method="m_7203_", at=@At("HEAD"))
    private void jem$begin(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) { BattleQuestArtifacts.begin(actor, actor.getItemInHand(hand), "sand"); }
    @Inject(method="m_7203_", at=@At("RETURN"))
    private void jem$end(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) { BattleQuestArtifacts.end(); }
}
