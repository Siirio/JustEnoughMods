package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.TideCatchProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.li64.tide.registries.entities.misc.LootCrateEntity", remap = false)
public abstract class RemainingTideCrateSpawnMixin {
    @Redirect(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$acceptedCrate(Level level, Entity crate) {
        boolean accepted = level.addFreshEntity(crate);
        TideCatchProof.crate(crate, accepted);
        return accepted;
    }
}
