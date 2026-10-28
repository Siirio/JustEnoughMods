package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.item.MagneticWeaponEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=MagneticWeaponEntity.class,remap=false)
public abstract class CaveQuestRemoteMiningMixin {
    @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean jem$actualRemoteMining(Level level,BlockPos pos,boolean drops){
        var tool=(MagneticWeaponEntity)(Object)this;var actor=CaveQuestProof.actor(tool.getController());
        boolean observed=actor!=null && CaveQuestProof.permitted(actor,pos) && !level.getBlockState(pos).isAir() && CaveQuestProof.item(actor.getUseItem(),"galena_gauntlet") && !tool.getItemStack().isEmpty();
        boolean result=level.destroyBlock(pos,drops);
        if(result && observed && level.getBlockState(pos).isAir())CaveQuestProof.emit(actor,"v3_cave_remote_work_mine");
        return result;
    }
}
