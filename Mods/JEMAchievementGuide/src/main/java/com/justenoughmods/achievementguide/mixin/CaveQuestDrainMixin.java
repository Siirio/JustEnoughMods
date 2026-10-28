package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.DrainBlock.class,remap=false)
public abstract class CaveQuestDrainMixin {
    @Shadow private int removeWaterBreadthFirstSearch(Level level,BlockPos pos){throw new AssertionError();}
    @Inject(method="drainLogic",at=@At("HEAD"))private void jem$begin(BlockState state,Level world,BlockPos pos,CallbackInfo ci){
        CaveQuestDrainRun.end();if(state.getValue(com.github.alexmodguy.alexscaves.server.block.DrainBlock.OPEN))CaveQuestDrainRun.begin(world,pos);
    }
    @Redirect(method="drainLogic",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/block/DrainBlock;removeWaterBreadthFirstSearch(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"))
    private int jem$actualDrain(com.github.alexmodguy.alexscaves.server.block.DrainBlock owner,Level world,BlockPos pos){
        var before=world.getFluidState(pos);int result=removeWaterBreadthFirstSearch(world,pos);var run=CaveQuestDrainRun.current();
        if(run!=null && result>0 && !before.isEmpty() && world.getFluidState(pos).isEmpty() && pos.getY()>run.drain.getY()){run.removed=true;run.fluid=before.getType();}
        return result;
    }
    @Redirect(method="drainLogic",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$actualFill(Level level,BlockPos pos,BlockState state){
        var before=level.getFluidState(pos);boolean result=level.setBlockAndUpdate(pos,state);var run=CaveQuestDrainRun.current();
        if(result && run!=null && run.level==level && run.removed && pos.getY()<run.drain.getY() && before.isEmpty()
                && !level.getFluidState(pos).isEmpty() && level.getFluidState(pos).getType().isSame(run.fluid))CaveQuestProof.emit(run.actor,"v3_cave_water_tools");
        return result;
    }
    @Inject(method="drainLogic",at=@At("RETURN"))private void jem$end(BlockState state,Level world,BlockPos pos,CallbackInfo ci){CaveQuestDrainRun.end();}
}
