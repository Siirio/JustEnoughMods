package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.CandicornEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=CandicornEntity.class,remap=false)
public abstract class CaveQuestCandicornMixin {
    @Unique private boolean jem$caveTameBefore;
    @Unique private int jem$caveAppleBefore;
    @Unique private float jem$caveChargeBefore;
    @Inject(method="m_6071_",at=@At("HEAD"))private void jem$beforeFeed(Player actor,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var corn=(CandicornEntity)(Object)this;jem$caveTameBefore=corn.isTame();jem$caveAppleBefore=CaveQuestProof.item(actor.getItemInHand(hand),"caramel_apple")?actor.getItemInHand(hand).getCount():0;}
    @Inject(method="m_6071_",at=@At("RETURN"))private void jem$afterFeed(Player actor,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var corn=(CandicornEntity)(Object)this;if(!jem$caveTameBefore && corn.isTame() && jem$caveAppleBefore>0 && jem$caveAppleBefore-actor.getItemInHand(hand).getCount()==1 && actor.getUUID().equals(corn.getOwnerUUID()) && CaveQuestProof.actor(actor)!=null)corn.getPersistentData().putUUID("jemCaveCandicornTameV4",actor.getUUID());}
    @Inject(method="onKeyPacket",at=@At("HEAD"))private void jem$beforeCharge(Entity actor,int type,CallbackInfo ci){jem$caveChargeBefore=((CandicornEntity)(Object)this).getMeterAmount();}
    @Inject(method="onKeyPacket",at=@At("RETURN"))private void jem$afterCharge(Entity actor,int type,CallbackInfo ci){var corn=(CandicornEntity)(Object)this;if(type==2 && jem$caveChargeBefore>=1 && corn.getMeterAmount()<jem$caveChargeBefore && corn.getControllingPassenger()==actor && CaveQuestProof.actor(actor)!=null)corn.getPersistentData().putUUID("jemCaveCandicornChargeV4",actor.getUUID());}
    @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$actualChargeHit(LivingEntity target,DamageSource source,float amount){
        var corn=(CandicornEntity)(Object)this;var actor=CaveQuestProof.actor(corn.getControllingPassenger());var proof=corn.getPersistentData();
        boolean valid=actor!=null && corn.isCharging() && corn.isSaddled() && actor.getUUID().equals(corn.getOwnerUUID()) && proof.hasUUID("jemCaveCandicornTameV4")
                && proof.getUUID("jemCaveCandicornTameV4").equals(actor.getUUID()) && proof.hasUUID("jemCaveCandicornChargeV4") && proof.getUUID("jemCaveCandicornChargeV4").equals(actor.getUUID()) && CaveQuestProof.permitted(actor,target.blockPosition());
        return valid?CaveQuestProof.damage(target,source,amount,actor,"v3_cave_candicorn_charge"):target.hurt(source,amount);
    }
}
