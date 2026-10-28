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

@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.item.SugarStaffHexEntity.class,remap=false)
public abstract class CaveQuestSugarHexMixin {
    @Redirect(method="hurtEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$nativeHex(LivingEntity target,DamageSource damage,float amount){
        var hex=(com.github.alexmodguy.alexscaves.server.entity.item.SugarStaffHexEntity)(Object)this;
        return CaveQuestProof.damage(target,damage,amount,hex.getOwner(),"v3_cave_candy_arsenal_staff");
    }
}
