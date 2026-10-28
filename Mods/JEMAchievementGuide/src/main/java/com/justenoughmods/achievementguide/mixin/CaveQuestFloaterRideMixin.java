package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.item.FloaterEntity.class,remap=false)
public abstract class CaveQuestFloaterRideMixin {
    @Inject(method="m_19956_",at=@At("RETURN"))private void jem$actualRide(Entity rider,Entity.MoveFunction function,CallbackInfo ci){
        var floater=(Entity)(Object)this;var actor=CaveQuestProof.actor(rider);var proof=floater.getPersistentData();
        if(actor!=null && actor.getRootVehicle()==floater && floater.isInWater() && proof.hasUUID("jemCaveFloaterV4") && proof.getUUID("jemCaveFloaterV4").equals(actor.getUUID())
                && actor.getY()-proof.getDouble("jemCaveFloaterYV4")>=4) {CaveQuestProof.emit(actor,"v3_cave_water_tools_floater");proof.remove("jemCaveFloaterV4");}
    }
}
