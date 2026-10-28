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

@Mixin(value=com.github.alexmodguy.alexscaves.server.item.DreadbowItem.class,remap=false)
public abstract class CaveQuestDreadbowSpawnMixin {
    @Redirect(method="m_5551_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeVolley(net.minecraft.world.level.Level level,Entity arrow,net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.level.Level original,LivingEntity user,int remaining){
        boolean result=level.addFreshEntity(arrow);
        if(result && CaveQuestProof.actor(user)!=null && arrow instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner()==user)
            arrow.getPersistentData().putUUID("jemCaveVolleyV4",user.getUUID());
        return result;
    }
}
