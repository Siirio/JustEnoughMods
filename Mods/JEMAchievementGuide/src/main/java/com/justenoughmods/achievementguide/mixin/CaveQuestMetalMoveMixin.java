package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.item.AbstractMovingBlockEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.MovingMetalBlockEntity;
import com.github.alexmodguy.alexscaves.server.block.blockentity.MagnetBlockEntity;
import com.github.alexmodguy.alexscaves.server.block.MagnetBlock;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=AbstractMovingBlockEntity.class,remap=false)
public abstract class CaveQuestMetalMoveMixin {
    @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/entity/item/AbstractMovingBlockEntity;m_6478_(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void jem$actualMove(AbstractMovingBlockEntity entity,MoverType type,Vec3 delta){
        Vec3 before=entity.position();entity.move(type,delta);
        if(!(entity instanceof MovingMetalBlockEntity) || !(entity.level() instanceof ServerLevel level) || entity.position().distanceToSqr(before)<=1.0e-8)return;
        var proof=entity.getPersistentData().getCompound("jemCaveMagneticMoveV4");var pos=BlockPos.of(proof.getLong("magnet"));
        if(!proof.hasUUID("actor") || level.getGameTime()<proof.getLong("time") || level.getGameTime()-proof.getLong("time")>1 || !CaveQuestProof.loaded(level,pos)
                || !(level.getBlockEntity(pos) instanceof MagnetBlockEntity magnet) || !magnet.getBlockState().getValue(MagnetBlock.POWERED))return;
        var actor=CaveQuestProof.owner(magnet);
        if(actor!=null && actor.level()==level && proof.getUUID("actor").equals(actor.getUUID()) && proof.getBoolean("azure")==magnet.isAzure()){
            CaveQuestProof.emit(actor,magnet.isAzure()?"v3_cave_magnet_workshop":"v3_cave_magnet_workshop_scarlet");entity.getPersistentData().remove("jemCaveMagneticMoveV4");
        }
    }
}
