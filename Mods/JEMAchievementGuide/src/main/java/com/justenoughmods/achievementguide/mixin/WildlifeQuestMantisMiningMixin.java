package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.ai.MantisShrimpAIBreakBlocks", remap = false)
public abstract class WildlifeQuestMantisMiningMixin {
    @Shadow @Final private EntityMantisShrimp mantisShrimp;
    @Redirect(method = "breakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean jem$mined(Level level, BlockPos pos, boolean drops) {
        var before = level.getBlockState(pos); var sample = mantisShrimp.getMainHandItem().copy(); boolean accepted = level.destroyBlock(pos, drops);
        var actor = WildlifeQuestProof.owner(mantisShrimp);
        if (accepted && actor != null && mantisShrimp.getCommand() == 3 && before.getBlock().asItem() == sample.getItem()
                && !sample.isEmpty() && before.getBlock() != level.getBlockState(pos).getBlock()) WildlifeQuestProof.emit(actor, "v3_fauna_animal_work");
        return accepted;
    }
}
