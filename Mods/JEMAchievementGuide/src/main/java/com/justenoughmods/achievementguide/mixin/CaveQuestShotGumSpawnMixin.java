package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=com.github.alexmodguy.alexscaves.server.item.ShotGumItem.class,remap=false)
public abstract class CaveQuestShotGumSpawnMixin {
    @Redirect(method="m_6883_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeShot(net.minecraft.world.level.Level level,Entity ball,net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.level.Level original,Entity user,int slot,boolean held){
        boolean result=level.addFreshEntity(ball);
        if(result && CaveQuestProof.actor(user)!=null && ball instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner()==user)
            ball.getPersistentData().putUUID("jemCaveShotGumV4",user.getUUID());
        return result;
    }
}
