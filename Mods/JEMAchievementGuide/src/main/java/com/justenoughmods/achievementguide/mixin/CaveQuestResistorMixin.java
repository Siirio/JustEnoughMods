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

@Mixin(value=com.github.alexmodguy.alexscaves.server.item.ResistorShieldItem.class,remap=false)
public abstract class CaveQuestResistorMixin {
    @Redirect(method="m_5929_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_147240_(DDD)V"))
    private void jem$nativePulse(LivingEntity target,double strength,double x,double z,net.minecraft.world.level.Level level,
            LivingEntity user,net.minecraft.world.item.ItemStack shield,int time){
        var before=target.getDeltaMovement(); target.knockback(strength,x,z); var after=target.getDeltaMovement();
        if(CaveQuestProof.hostile(target,user) && Double.isFinite(after.lengthSqr()) && after.distanceToSqr(before)>1.0e-10)
            CaveQuestProof.emit(user,com.github.alexmodguy.alexscaves.server.item.ResistorShieldItem.isScarlet(shield)?"v3_cave_resistor_scarlet":"v3_cave_resistor");
    }
}
