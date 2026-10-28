package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LevelChunk.class)
public abstract class WorldPrehistoricQuestChunkMixin {
 @Inject(method="setBlockState",at=@At("RETURN"))private void jem$changed(BlockPos pos,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> ci){var before=ci.getReturnValue();if(before!=null){WorldPrehistoricQuestAsync.changed(before,state);if(before.getBlock()!=state.getBlock())WorldPrehistoricQuestOrigins.remove((LevelChunk)(Object)this,pos,true);}}
}
