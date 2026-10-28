package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.item.SubmarineEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=SubmarineEntity.class,remap=false)
public abstract class CaveQuestSubmarineLightMixin {
    @Unique private boolean jem$caveLights;
    @Inject(method="onKeyPacket",at=@At("HEAD"))private void jem$before(Entity user,int type,CallbackInfo ci){jem$caveLights=((SubmarineEntity)(Object)this).areLightsOn();}
    @Inject(method="onKeyPacket",at=@At("RETURN"))private void jem$after(Entity user,int type,CallbackInfo ci){
        var boat=(SubmarineEntity)(Object)this;boat.getPersistentData().remove("jemCaveFloodlightOffV4");
        if(type==2 && jem$caveLights && !boat.areLightsOn() && user.getRootVehicle()==boat && CaveQuestProof.actor(user)!=null)boat.getPersistentData().putUUID("jemCaveFloodlightOffV4",user.getUUID());
    }
}
