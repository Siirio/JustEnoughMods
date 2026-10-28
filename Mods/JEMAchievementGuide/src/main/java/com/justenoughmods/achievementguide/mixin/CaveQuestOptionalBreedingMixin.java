package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.AnimalBreedEggsGoal.class,remap=false)
public abstract class CaveQuestOptionalBreedingMixin {
    @Shadow @Final private Animal mob;
    @Unique private java.util.UUID jem$caveBreeder,jem$caveMate;
    @Inject(method="m_8026_",at=@At("HEAD"))private void jem$before(CallbackInfo ci){
        jem$caveBreeder=null;jem$caveMate=null;var partner=((CaveQuestBreedPairAccess)(Object)this).jem$cavePartner();
        if(mob instanceof TremorzillaEntity first && partner instanceof TremorzillaEntity second && !first.hasEgg()){
            var actor=CaveQuestProof.owner(first,"jemCaveBombBreederV4");
            if(actor!=null && second.getPersistentData().hasUUID("jemCaveBombBreederV4") && second.getPersistentData().getUUID("jemCaveBombBreederV4").equals(actor.getUUID())
                    && first.getLoveCause()==actor && second.getLoveCause()==actor){jem$caveBreeder=actor.getUUID();jem$caveMate=second.getUUID();}
        }
    }
    @Inject(method="m_8026_",at=@At("RETURN"))private void jem$viable(CallbackInfo ci){
        if(jem$caveBreeder!=null && mob instanceof TremorzillaEntity giant && giant.hasEgg()){
            mob.getPersistentData().putUUID("jemCaveViableEggActorV4",jem$caveBreeder);mob.getPersistentData().putUUID("jemCaveViableEggMateV4",jem$caveMate);
        }jem$caveBreeder=null;jem$caveMate=null;
    }
}
