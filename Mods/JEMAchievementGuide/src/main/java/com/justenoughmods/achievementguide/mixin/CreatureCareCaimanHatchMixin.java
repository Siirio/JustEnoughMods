package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.block.BlockReptileEgg", remap = false)
public abstract class CreatureCareCaimanHatchMixin {
    @Redirect(method = "m_213898_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeImprint(ServerLevel level, Entity entity) {
        boolean accepted = level.addFreshEntity(entity);
        if (accepted && entity instanceof TamableAnimal baby && baby.isBaby() && baby.isTame()
                && CreatureCareProof.type(baby).equals("alexsmobs:caiman") && baby.getOwnerUUID() != null) {
            CreatureCareProof.mark(baby, "caiman_hatched", baby.getOwnerUUID());
        }
        return accepted;
    }
}
