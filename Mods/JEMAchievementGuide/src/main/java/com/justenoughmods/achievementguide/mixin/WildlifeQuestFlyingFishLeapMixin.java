package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestMotion;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestTools;
import com.github.alexthe666.alexsmobs.entity.util.FlyingFishBootsUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.util.FlyingFishBootsUtil", remap = false)
public abstract class WildlifeQuestFlyingFishLeapMixin {
    @Unique private static final ThreadLocal<Integer> jem$boost = new ThreadLocal<>();
    @Inject(method = "tickFlyingFishBoots", at = @At("HEAD"))
    private static void jem$before(LivingEntity entity, CallbackInfo callback) { jem$boost.set(FlyingFishBootsUtil.getBoostTicks(entity)); }
    @Inject(method = "tickFlyingFishBoots", at = @At("RETURN"))
    private static void jem$boosted(LivingEntity entity, CallbackInfo callback) {
        Integer before = jem$boost.get(); jem$boost.remove();
        if (entity instanceof ServerPlayer actor && WildlifeQuestProof.real(actor) != null && before != null && before <= 15
                && FlyingFishBootsUtil.getBoostTicks(actor) == 34 && actor.getDeltaMovement().y > 0 && actor.isInWater()) {
            var feet = actor.getItemBySlot(EquipmentSlot.FEET);
            if (actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(feet, "wildlife_boots"))) WildlifeQuestMotion.waterLeap(actor, WildlifeQuestTools.get(actor.serverLevel()).origin(feet));
        }
    }
}
