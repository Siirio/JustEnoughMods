package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.AcidBlock.class,remap=false)
public abstract class CaveQuestCorrosionMixin {
    @Redirect(method="tickCorrosion",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$actualRust(Level world,BlockPos pos,BlockState state){
        var origin=world instanceof ServerLevel level?CaveQuestBlockOrigins.get(level,pos,"Placed"):new net.minecraft.nbt.CompoundTag();var before=world.getBlockState(pos);
        boolean result=world.setBlockAndUpdate(pos,state);
        if(result && world instanceof ServerLevel level && before.getBlock()!=state.getBlock() && level.getBlockState(pos).equals(state)
                && String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())).startsWith("alexscaves:rusty_")){
            var actor=origin.hasUUID("actor")?CaveQuestProof.actor(level,origin.getUUID("actor")):null;
            if(CaveQuestProof.hazmat(actor))CaveQuestProof.emit(actor,"v3_cave_hazmat_processing");
            var run=CaveQuestPlacementRun.current();if(run!=null && run.actor!=null && run.actor.level()==world && run.pos.equals(pos) && run.block==before.getBlock())run.rust=state;
        }return result;
    }
}
