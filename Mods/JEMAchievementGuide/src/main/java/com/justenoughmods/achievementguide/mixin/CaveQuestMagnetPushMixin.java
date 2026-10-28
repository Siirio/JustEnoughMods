package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.block.blockentity.MagnetBlockEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.MovingMetalBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=MagnetBlockEntity.class,remap=false)
public abstract class CaveQuestMagnetPushMixin {
    @Unique private Vec3 jem$caveVelocity;
    @Inject(method="pushEntity",at=@At("HEAD"))private void jem$before(Entity entity,CallbackInfo ci){jem$caveVelocity=entity.getDeltaMovement();}
    @Inject(method="pushEntity",at=@At("RETURN"))private void jem$pushed(Entity entity,CallbackInfo ci){
        var magnet=(MagnetBlockEntity)(Object)this;var actor=CaveQuestProof.owner(magnet);
        if(actor!=null && entity instanceof MovingMetalBlockEntity metal && !metal.getData().isEmpty() && metal.getData().size()<=27
                && jem$caveVelocity!=null && entity.getDeltaMovement().distanceToSqr(jem$caveVelocity)>1.0e-10){
            long time=actor.level().getGameTime();var data=entity.getPersistentData();var previous=data.getCompound("jemCaveMagneticMoveV4");
            if(data.contains("jemCaveMagneticConflictV4") && data.getLong("jemCaveMagneticConflictV4")==time)return;
            if(previous.getLong("time")==time && previous.hasUUID("actor") && (!previous.getUUID("actor").equals(actor.getUUID()) || previous.getLong("magnet")!=magnet.getBlockPos().asLong())){data.remove("jemCaveMagneticMoveV4");data.putLong("jemCaveMagneticConflictV4",time);return;}
            CompoundTag proof=new CompoundTag();proof.putUUID("actor",actor.getUUID());proof.putLong("magnet",magnet.getBlockPos().asLong());proof.putBoolean("azure",magnet.isAzure());
            proof.putLong("time",actor.level().getGameTime());entity.getPersistentData().put("jemCaveMagneticMoveV4",proof);
        }
        jem$caveVelocity=null;
    }
}
