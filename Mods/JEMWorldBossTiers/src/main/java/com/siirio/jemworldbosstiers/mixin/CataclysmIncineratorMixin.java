package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.IncineratorBudget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.items.The_Incinerator", remap = false)
public abstract class CataclysmIncineratorMixin {
    @Inject(method = "m_5551_", at = @At("HEAD"), require = 1, remap = false)
    private void jem$castIdentity(ItemStack stack, Level level, LivingEntity caster, int remaining, CallbackInfo callback) {
        IncineratorBudget.begin(caster);
    }

    @Redirect(method = "spawnFlameStrike",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true),
            require = 1, expect = 1, remap = false)
    private boolean jem$tagZone(Level world, Entity zone, double x, double z, double minY, double maxY,
                               float rotation, int duration, int wait, int warmup, Level level,
                               float radius, LivingEntity caster) {
        IncineratorBudget.tag(zone, caster);
        return world.addFreshEntity(zone);
    }
}
