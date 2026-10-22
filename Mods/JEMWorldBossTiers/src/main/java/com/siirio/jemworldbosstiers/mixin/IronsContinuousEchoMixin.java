package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.irons.ContinuousEchoes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.effect.EchoingStrikesEffect", remap = false)
public abstract class IronsContinuousEchoMixin {
    @Redirect(method = "createEcho(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true),
            require = 1, expect = 1, remap = false)
    private static boolean jem$mergeContinuous(Level world, Entity echo, LivingHurtEvent event) {
        return ContinuousEchoes.spawn(world, echo, event);
    }
}
