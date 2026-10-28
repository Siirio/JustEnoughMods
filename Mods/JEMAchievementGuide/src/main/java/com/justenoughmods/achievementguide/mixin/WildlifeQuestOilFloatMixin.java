package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.effect.EffectOiled", remap = false)
public abstract class WildlifeQuestOilFloatMixin {
    @Redirect(method = "m_6742_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"))
    private void jem$float(LivingEntity entity, Vec3 motion) {
        Vec3 before = entity.getDeltaMovement(); entity.setDeltaMovement(motion);
        if (entity instanceof ServerPlayer actor && WildlifeQuestProof.real(actor) != null && motion.y > before.y && actor.getDeltaMovement().y > before.y) {
            var proof = WildlifeQuestProof.data(actor);
            if (proof.hasUUID("oiled_craft") && proof.getLong("oiled_until") >= actor.level().getGameTime()
                    && WildlifeQuestTools.consume(actor, "wildlife_oil", proof.getUUID("oiled_craft"))) { WildlifeQuestProof.emit(actor, "v3_fauna_ocean_finds"); proof.remove("oiled_craft"); }
        }
    }
}
