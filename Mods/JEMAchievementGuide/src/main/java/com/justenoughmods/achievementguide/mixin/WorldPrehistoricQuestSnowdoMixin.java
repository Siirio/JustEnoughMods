package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import team.recrafted.blastfromthepast.entity.SnowdoEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=SnowdoEntity.class,remap=false)
public abstract class WorldPrehistoricQuestSnowdoMixin {
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$shear(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var bird=(SnowdoEntity)(Object)this;WorldPrehistoricQuestDrops.beginShear(WorldPrehistoricQuestProof.actor(p),bird,bird.isSheared());}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$sheared(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){WorldPrehistoricQuestDrops.finishShear(((SnowdoEntity)(Object)this).isSheared());}
 @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"))private void jem$nativeFall(Player p,Vec3 motion){var actor=WorldPrehistoricQuestProof.actor(p);var bird=(SnowdoEntity)(Object)this;var before=p.getDeltaMovement();p.setDeltaMovement(motion);if(actor!=null && bird.isAlive() && bird.getVehicle()==actor && !actor.onGround() && before.y<0 && motion.y<0 && motion.y>before.y && p.getDeltaMovement().equals(motion))WorldPrehistoricQuestProof.emit(actor,"v3_era_soft_landing_fall");}
}
