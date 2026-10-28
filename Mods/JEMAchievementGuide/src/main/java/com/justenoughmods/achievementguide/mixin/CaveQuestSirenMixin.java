package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearSirenBlockEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearBombEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=NuclearSirenBlockEntity.class,remap=false)
public abstract class CaveQuestSirenMixin implements com.justenoughmods.achievementguide.integration.caves.CaveQuestSirenReceipt {
    @Shadow private Entity nearestNuclearBomb;
    @Shadow private int bombId;
    @Inject(method="tick",at=@At("RETURN"))private static void jem$nativeAlarm(Level world,BlockPos pos,BlockState state,NuclearSirenBlockEntity siren,CallbackInfo ci){((com.justenoughmods.achievementguide.integration.caves.CaveQuestSirenReceipt)(Object)siren).jem$caveAlarm();}
    @Override public void jem$caveAlarm(){
        var siren=(NuclearSirenBlockEntity)(Object)this;var actor=CaveQuestProof.owner(siren);
        // Installed nuclear_siren sound JSON defines native LINEAR attenuation_distance 256.
        if(actor!=null && actor.level()==siren.getLevel() && nearestNuclearBomb instanceof NuclearBombEntity bomb && bombId==bomb.getId() && !bomb.isRemoved() && bomb.getTime()<NuclearBombEntity.MAX_TIME
                && bomb.getPersistentData().hasUUID("jemCaveIgniterV4") && siren.isActivated(siren.getBlockState()) && siren.getVolume(1)>0 && actor.position().distanceToSqr(siren.getBlockPos().getCenter())<=256.0*256.0)
            CaveQuestProof.emit(actor,"v3_cave_nuclear_controls_siren");
    }
}
