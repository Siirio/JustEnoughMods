package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityToucan;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityToucan$AIPlantTrees", remap = false)
public abstract class WildlifeQuestToucanPlantMixin {
    @Shadow @Final protected EntityToucan toucan;
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$planted(Level level, BlockPos pos, BlockState state) {
        var before = level.getBlockState(pos); boolean accepted = level.setBlockAndUpdate(pos, state); var actor = WildlifeQuestProof.actor(toucan, "tree_actor");
        if (actor != null && accepted && before != state && level.getBlockState(pos).equals(state)
                && NbtUtils.writeBlockState(state).equals(WildlifeQuestProof.data(toucan).getCompound("tree_state"))) WildlifeQuestProof.emit(actor, "v3_fauna_pollinators_toucan");
        return accepted;
    }
}
