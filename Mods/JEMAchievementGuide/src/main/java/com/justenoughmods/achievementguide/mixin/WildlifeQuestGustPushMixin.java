package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityGust", remap = false)
public abstract class WildlifeQuestGustPushMixin {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"))
    private void jem$push(Entity entity, Vec3 motion) { var before = entity.getDeltaMovement(); entity.setDeltaMovement(motion); WildlifeQuestGust.pushed((Entity)(Object)this, entity, before, entity.getDeltaMovement()); }
}
