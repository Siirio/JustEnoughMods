package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestBlockOrigins;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.SulfurBudBlock.class,remap=false)
public abstract class CaveQuestSulfurGrowthMixin {
    @Redirect(method="m_213898_",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$actualMaturity(ServerLevel level,BlockPos pos,BlockState state){
        var before=level.getBlockState(pos);boolean result=level.setBlockAndUpdate(pos,state);
        if(result && before.is(com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry.SULFUR_BUD_LARGE.get()) && level.getBlockState(pos).is(com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry.SULFUR_CLUSTER.get())){
            var proof=new net.minecraft.nbt.CompoundTag();proof.put("state",net.minecraft.nbt.NbtUtils.writeBlockState(level.getBlockState(pos)));CaveQuestBlockOrigins.put(level,pos,"Grown",proof);
        }return result;
    }
}
