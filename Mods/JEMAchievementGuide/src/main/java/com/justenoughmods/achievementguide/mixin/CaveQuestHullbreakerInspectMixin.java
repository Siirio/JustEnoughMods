package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.item.SubmarineEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.HullbreakerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.HullbreakerInspectMobGoal.class,remap=false)
public abstract class CaveQuestHullbreakerInspectMixin {
    @Shadow private HullbreakerEntity entity;
    @Shadow private LivingEntity inspectingTarget;
    @Unique private ServerPlayer jem$caveInspected;
    @Unique private SubmarineEntity jem$caveBoat;
    @Inject(method="m_8037_",at=@At("HEAD"))private void jem$before(CallbackInfo ci){
        jem$caveInspected=null;jem$caveBoat=null;var actor=CaveQuestProof.actor(inspectingTarget);
        if(actor!=null && actor.getRootVehicle() instanceof SubmarineEntity boat && !boat.areLightsOn() && boat.getPersistentData().hasUUID("jemCaveFloodlightOffV4")
                && boat.getPersistentData().getUUID("jemCaveFloodlightOffV4").equals(actor.getUUID()) && !HullbreakerEntity.GLOWING_TARGET.test(actor)){jem$caveInspected=actor;jem$caveBoat=boat;}
    }
    @Inject(method="m_8037_",at=@At("RETURN"))private void jem$lostInterest(CallbackInfo ci){
        if(jem$caveInspected!=null && inspectingTarget==null && entity.getTarget()==null && jem$caveInspected.getRootVehicle()==jem$caveBoat && !jem$caveBoat.areLightsOn()){
            CaveQuestProof.emit(jem$caveInspected,"v3_cave_cave_counterplay_hullbreaker");jem$caveBoat.getPersistentData().remove("jemCaveFloodlightOffV4");}
        jem$caveInspected=null;jem$caveBoat=null;
    }
}
