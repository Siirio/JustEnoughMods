package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.RelicheirusEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.FallingTreeBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashSet;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.RelicheirusPushTreesGoal.class,remap=false)
public abstract class CaveQuestFellingMixin {
    @Shadow private RelicheirusEntity relicheirus;
    @Unique private final Set<BlockPos> jem$caveRemoved=new HashSet<>();
    @Inject(method="m_8037_",at=@At("HEAD"))private void jem$begin(CallbackInfo ci){jem$caveRemoved.clear();}
    @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jem$nativeTreeRemoval(Level level,BlockPos pos,BlockState state,int flags){
        boolean was=!level.getBlockState(pos).isAir();boolean result=level.setBlock(pos,state,flags);
        if(result && was && level.getBlockState(pos).isAir() && jem$caveRemoved.size()<301)jem$caveRemoved.add(pos.immutable());return result;
    }
    @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeFallingTree(Level level,Entity tree){
        boolean result=level.addFreshEntity(tree);var actor=CaveQuestProof.owner(relicheirus,"jemCaveSoupFeederV4");
        if(result && actor!=null && tree instanceof FallingTreeBlockEntity falling && !jem$caveRemoved.isEmpty() && jem$caveRemoved.size()<=300 && falling.getData().size()==jem$caveRemoved.size()){
            tree.getPersistentData().putUUID("jemCaveFellingActorV4",actor.getUUID());tree.getPersistentData().putUUID("jemCaveFellingAnimalV4",relicheirus.getUUID());
        }
        return result;
    }
}
