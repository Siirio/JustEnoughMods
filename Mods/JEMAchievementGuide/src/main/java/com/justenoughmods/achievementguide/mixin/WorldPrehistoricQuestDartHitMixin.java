package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="net.mcreator.unearthedjourney.entity.TippedBlowDartProjectileEntity",remap=false)
public abstract class WorldPrehistoricQuestDartHitMixin {
 @Inject(method="m_5790_",at=@At("HEAD"))private void jem$before(EntityHitResult result,CallbackInfo ci){WorldPrehistoricQuestDart.beforeHit((Projectile)(Object)this,result.getEntity());}
 @Inject(method="m_5790_",at=@At("RETURN"))private void jem$after(EntityHitResult result,CallbackInfo ci){WorldPrehistoricQuestDart.afterHit((Projectile)(Object)this);}
}
