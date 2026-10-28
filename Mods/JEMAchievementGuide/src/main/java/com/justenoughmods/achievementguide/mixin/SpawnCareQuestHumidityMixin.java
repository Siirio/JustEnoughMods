package com.justenoughmods.achievementguide.mixin;
import com.ninni.spawn.server.block.MucusBlockBlock;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.ninni.spawn.server.block.MucusBlockBlock", remap = false)
public abstract class SpawnCareQuestHumidityMixin {
    @Redirect(method = "m_213897_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jem$humidity(ServerLevel level, BlockPos pos, BlockState actual, int flags) {
        var before = level.getBlockState(pos); boolean accepted = level.setBlock(pos, actual, flags); var proof = SpawnCareQuestSetups.get(level).at(level, pos);
        if (accepted && before.getBlock() == actual.getBlock() && level.getBlockState(pos).equals(actual) && before.hasProperty(MucusBlockBlock.SOLID) && before.getValue(MucusBlockBlock.SOLID) != actual.getValue(MucusBlockBlock.SOLID) && proof.hasUUID("actor") && proof.getBoolean("after_wet_collection")) {
            var actor = SpawnCareQuestProof.real(level.getServer().getPlayerList().getPlayer(proof.getUUID("actor"))); if (actor != null) SpawnCareQuestProof.emit(actor, "v3_pet_wet_work_humidity");
        } return accepted;
    }
}
