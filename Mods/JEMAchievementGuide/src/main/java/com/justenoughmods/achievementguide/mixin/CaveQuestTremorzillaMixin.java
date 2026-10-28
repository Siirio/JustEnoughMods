package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=TremorzillaEntity.class,remap=false)
public abstract class CaveQuestTremorzillaMixin {
    @Shadow private Player lastFedPlayer;
    @Unique private int jem$drumsBefore,jem$bombsBefore;
    @Unique private boolean jem$loveBefore;
    @Unique private boolean jem$wasTame,jem$beamBefore;
    @Unique private Player jem$actualFeeder;
    @Inject(method="m_6071_",at=@At("HEAD"))private void jem$feeding(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$loveBefore=((TremorzillaEntity)(Object)this).isInLove();jem$bombsBefore=CaveQuestProof.item(player.getItemInHand(hand),"nuclear_bomb")?player.getItemInHand(hand).getCount():0;jem$drumsBefore=CaveQuestProof.item(player.getItemInHand(hand),"waste_drum")?player.getItemInHand(hand).getCount():0;}
    @Inject(method="m_6071_",at=@At("RETURN"))private void jem$fed(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){
        var giant=(TremorzillaEntity)(Object)this;
        if(!jem$loveBefore && giant.isInLove() && jem$bombsBefore>0 && jem$bombsBefore-player.getItemInHand(hand).getCount()==1){giant.getPersistentData().remove("jemCaveBombBreederV4");if(CaveQuestProof.actor(player)!=null && giant.getLoveCause()==player)giant.getPersistentData().putUUID("jemCaveBombBreederV4",player.getUUID());}
        if(jem$drumsBefore>0 && jem$drumsBefore-player.getItemInHand(hand).getCount()==1 && lastFedPlayer==player && CaveQuestProof.actor(player)!=null)
            giant.getPersistentData().putUUID("jemCaveDrumFeederV4",player.getUUID());
    }
    @Inject(method="m_8119_",at=@At("HEAD"))private void jem$beforeTame(CallbackInfo ci){var giant=(TremorzillaEntity)(Object)this;jem$wasTame=giant.isTame();jem$actualFeeder=lastFedPlayer;}
    @Inject(method="m_8119_",at=@At("RETURN"))private void jem$nativeTamed(CallbackInfo ci){
        var giant=(TremorzillaEntity)(Object)this;var proof=giant.getPersistentData();
        if(!jem$wasTame && giant.isTame() && CaveQuestProof.actor(jem$actualFeeder)!=null && proof.hasUUID("jemCaveDrumFeederV4")
                && proof.getUUID("jemCaveDrumFeederV4").equals(giant.getOwnerUUID()) && jem$actualFeeder.getUUID().equals(giant.getOwnerUUID()))proof.putUUID("jemCaveNativeTameV4",giant.getOwnerUUID());
        jem$actualFeeder=null;
    }
    @Inject(method="onKeyPacket",at=@At("HEAD"))private void jem$beforeBeam(Entity user,int type,CallbackInfo ci){jem$beamBefore=((TremorzillaEntity)(Object)this).wantsToUseBeamFromServer;}
    @Inject(method="onKeyPacket",at=@At("RETURN"))private void jem$controlledBeam(Entity user,int type,CallbackInfo ci){
        var giant=(TremorzillaEntity)(Object)this;
        if(type==2 && !jem$beamBefore && giant.wantsToUseBeamFromServer && giant.getControllingPassenger()==user && CaveQuestProof.actor(user)!=null
                && user.getUUID().equals(giant.getOwnerUUID()))giant.getPersistentData().putUUID("jemCaveBeamActorV4",user.getUUID());
    }
    @Redirect(method="hurtEntitiesAround",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$nativeBeamDamage(LivingEntity target,DamageSource source,float amount,Vec3 center,float radius,float damage,float knockback,boolean radioactive,boolean otherKaiju,boolean stretch){
        var giant=(TremorzillaEntity)(Object)this;var proof=giant.getPersistentData();var actor=CaveQuestProof.actor(giant.getControllingPassenger());
        boolean valid=radioactive && giant.isFiring() && actor!=null && actor.getUUID().equals(giant.getOwnerUUID()) && proof.hasUUID("jemCaveHatchlingV4")
                && proof.getUUID("jemCaveHatchlingV4").equals(giant.getUUID()) && proof.hasUUID("jemCaveNuclearBirthV4")
                && proof.hasUUID("jemCaveNativeTameV4") && proof.getUUID("jemCaveNativeTameV4").equals(actor.getUUID())
                && proof.hasUUID("jemCaveBeamActorV4") && proof.getUUID("jemCaveBeamActorV4").equals(actor.getUUID());
        return valid?CaveQuestProof.damage(target,source,amount,actor,"v3_cave_giant_nursery"):target.hurt(source,amount);
    }
}
