package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.block.BlockGustmaker", remap = false)
public abstract class WildlifeQuestGustSpawnMixin {
    @Redirect(method = "tickGustmaker", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$gust(Level level, Entity gust, BlockState state, Level original, BlockPos pos, boolean off) {
        boolean accepted = level.addFreshEntity(gust);
        if (accepted && level instanceof ServerLevel server) { var proof = WildlifeQuestSetups.get(server).at(server, pos); if (proof.hasUUID("actor")) WildlifeQuestProof.mark(gust, "gust_actor", proof.getUUID("actor")); }
        return accepted;
    }
}
