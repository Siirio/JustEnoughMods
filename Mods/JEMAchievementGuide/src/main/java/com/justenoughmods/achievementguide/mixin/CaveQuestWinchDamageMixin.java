package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.BoundroidWinchEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BoundroidWinchEntity.class,remap=false)
public abstract class CaveQuestWinchDamageMixin {
    @Unique private boolean jem$caveLatched;
    @Inject(method="m_6469_",at=@At("HEAD"))private void jem$before(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){jem$caveLatched=((BoundroidWinchEntity)(Object)this).isLatched();}
    @Inject(method="m_6469_",at=@At("RETURN"))private void jem$after(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        var winch=(BoundroidWinchEntity)(Object)this;var actor=CaveQuestProof.actor(source.getEntity());var head=winch.getHead();
        if(actor!=null && ci.getReturnValue() && jem$caveLatched && Float.isFinite(amount) && amount>0 && head!=null && head.isAlive()){
            winch.getPersistentData().putUUID("jemCaveWinchActorV4",actor.getUUID());winch.getPersistentData().putUUID("jemCaveWinchHeadV4",head.getUUID());winch.getPersistentData().putDouble("jemCaveWinchHeadYV4",head.getY());}
    }
}
