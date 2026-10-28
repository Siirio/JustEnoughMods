package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.living.CorrodentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.CorrodentFearLightGoal.class,remap=false)
public abstract class CaveQuestCorrodentFearMixin {
    @Shadow private CorrodentEntity entity;
    @Unique private BlockPos jem$caveLamp;
    @Unique private Vec3 jem$caveFleeFrom;
    @Unique private java.util.UUID jem$caveLampActor;
    @Inject(method="m_8037_",at=@At("RETURN"))private void jem$actualFlee(CallbackInfo ci){
        if(!(entity.level() instanceof ServerLevel level) || !entity.isAfraid())return;
        if(jem$caveLamp!=null && CaveQuestProof.loaded(level,jem$caveLamp)){
            var proof=CaveQuestBlockOrigins.get(level,jem$caveLamp,"Placed");var actor=CaveQuestProof.actor(level,jem$caveLampActor);
            if(actor!=null && proof.hasUUID("actor") && proof.getUUID("actor").equals(actor.getUUID()) && level.getBlockState(jem$caveLamp).getLightEmission(level,jem$caveLamp)>7
                    && entity.position().distanceToSqr(jem$caveLamp.getCenter())>jem$caveFleeFrom.distanceToSqr(jem$caveLamp.getCenter())+1.0e-6){CaveQuestProof.emit(actor,"v3_cave_cave_counterplay_corrodent");jem$caveLamp=null;}
        }else{
            var pos=entity.blockPosition();var proof=CaveQuestBlockOrigins.get(level,pos,"Placed");
            if(proof.hasUUID("actor") && level.getBlockState(pos).getLightEmission(level,pos)>7){jem$caveLamp=pos.immutable();jem$caveLampActor=proof.getUUID("actor");jem$caveFleeFrom=entity.position();}
        }
    }
    @Inject(method="m_8041_",at=@At("RETURN"))private void jem$stopped(CallbackInfo ci){jem$caveLamp=null;jem$caveFleeFrom=null;jem$caveLampActor=null;}
}
