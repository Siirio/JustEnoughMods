package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.BoundroidEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.BoundroidWinchEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BoundroidEntity.class,remap=false)
public abstract class CaveQuestBoundroidFallMixin {
    @Inject(method="m_8119_",at=@At("RETURN"))private void jem$nativeFall(CallbackInfo ci){
        var head=(BoundroidEntity)(Object)this;
        if(head.getWinch() instanceof BoundroidWinchEntity winch && !winch.isLatched() && !head.stopGravity){var proof=winch.getPersistentData();var actor=CaveQuestProof.owner(winch,"jemCaveWinchActorV4");
            if(actor!=null && proof.hasUUID("jemCaveWinchHeadV4") && proof.getUUID("jemCaveWinchHeadV4").equals(head.getUUID()) && head.getY()<proof.getDouble("jemCaveWinchHeadYV4")-0.01){CaveQuestProof.emit(actor,"v3_cave_cave_counterplay");proof.remove("jemCaveWinchActorV4");}}
    }
}
