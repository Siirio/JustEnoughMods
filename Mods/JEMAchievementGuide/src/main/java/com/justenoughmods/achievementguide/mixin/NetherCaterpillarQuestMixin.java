package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.bmt.kaleidoscope_nether.item.NetherCaterpillarItem", remap = false)
public abstract class NetherCaterpillarQuestMixin {
    // The first RETURN is exclusively the native server baby-growth branch, after shrink(1) and setAge(0)
    @Inject(method = "m_6880_", at = @At(value = "RETURN", ordinal = 0))
    private void jem$afterGrowth(ItemStack stack, Player player, LivingEntity target, InteractionHand hand,
                                 CallbackInfoReturnable<InteractionResult> callback) {
        if (player instanceof ServerPlayer actor && callback.getReturnValue() == InteractionResult.SUCCESS
                && !actor.getAbilities().instabuild && target instanceof Strider strider && !strider.isBaby()) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_food_lava_nursery");
        }
    }
}
