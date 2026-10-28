package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.item.QuarrySmasherEntity;
import com.github.alexmodguy.alexscaves.server.block.blockentity.QuarryBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=QuarrySmasherEntity.class,remap=false)
public abstract class CaveQuestQuarryMixin {
    @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean jem$actualQuarryBreak(Level level,BlockPos pos,boolean drops){
        var smasher=(QuarrySmasherEntity)(Object)this;var quarryPos=smasher.getQuarryPos();
        var quarry=level instanceof net.minecraft.server.level.ServerLevel world && quarryPos!=null && CaveQuestProof.loaded(world,quarryPos) && level.getBlockEntity(quarryPos) instanceof QuarryBlockEntity owner?owner:null;
        var actor=quarry==null?null:CaveQuestProof.owner(quarry);
        boolean observed=actor!=null && actor.level()==level && quarry.hasMiningArea() && quarry.getMiningBox()!=null && pos.equals(smasher.getTargetPos())
                && QuarryBlockEntity.isMinable(level,pos) && CaveQuestProof.permitted(actor,pos);
        return CaveQuestDrops.observe(smasher,observed?actor:null,"quarry",()->level.destroyBlock(pos,drops),()->level.getBlockState(pos).isAir());
    }
    @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/item/ItemEntity;m_6034_(DDD)V"))
    private void jem$actualEjection(ItemEntity item,double x,double y,double z){
        item.setPos(x,y,z);var smasher=(QuarrySmasherEntity)(Object)this;var proof=CaveQuestDrops.proof(item);
        if(proof.getString("kind").equals("quarry") && proof.getUUID("source").equals(smasher.getUUID()) && item.level() instanceof net.minecraft.server.level.ServerLevel level){
            var pos=smasher.getQuarryPos();if(pos!=null && CaveQuestProof.loaded(level,pos) && level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry){var actor=CaveQuestProof.owner(quarry);
                if(actor!=null && actor.getUUID().equals(proof.getUUID("actor")) && item.position().distanceToSqr(pos.getCenter().add(0,0.5,0))<0.01){CaveQuestProof.emit(actor,"v3_cave_quarry_cycle");item.getPersistentData().remove(CaveQuestDrops.KEY);}}
        }
    }
}
