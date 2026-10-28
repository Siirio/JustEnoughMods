package com.justenoughmods.achievementguide.mixin;
import com.ninni.spawn.server.entity.mob.Snail;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.ninni.spawn.server.entity.mob.Snail", remap = false)
public abstract class SpawnCareQuestWetSnailTrailMixin {
    @Redirect(method = "m_8107_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$secretion(Level level, BlockPos pos, BlockState state) {
        boolean accepted = level.setBlockAndUpdate(pos, state); Snail self = (Snail)(Object)this;
        if (accepted && level instanceof ServerLevel server && self.getWetTicks() > 0 && SpawnCareQuestSetups.block(state).equals("spawn:mucus") && level.getBlockState(pos).equals(state)) {
            var proof = new CompoundTag(); proof.putUUID("wet_snail", self.getUUID()); proof.putString("block", "spawn:mucus"); SpawnCareQuestSetups.get(server).put(server, pos, proof);
        } return accepted;
    }
}
