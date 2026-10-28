package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.JugFoodRun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.satisfy.herbalbrews.core.blocks.JugBlock;
import net.satisfy.herbalbrews.core.blocks.entity.JugBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = JugBlock.class, remap = false)
public abstract class JugBlockFoodQuestMixin {
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/satisfy/herbalbrews/core/blocks/entity/JugBlockEntity;applyEffects(Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private void jem$nativeEffects(JugBlockEntity jug, LivingEntity actor, int duration) {
        ((JugFoodRun)jug).jem$applyForQuest(actor, duration);
    }

    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/satisfy/herbalbrews/core/blocks/entity/JugBlockEntity;clearDrinks()V"))
    private void jem$nativeConsumption(JugBlockEntity jug) {
        ServerPlayer player = ((JugFoodRun)jug).jem$consumeDrinker();
        jug.clearDrinks();
        if (player != null && jug.getDrinks().isEmpty()) QuestActionTrigger.INSTANCE.emit(player, "v3_food_tea_jug");
    }
}
