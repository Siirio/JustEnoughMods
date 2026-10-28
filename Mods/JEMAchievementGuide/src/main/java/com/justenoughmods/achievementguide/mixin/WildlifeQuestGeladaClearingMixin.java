package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestContext;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityGeladaMonkey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityGeladaMonkey$AIClearGrass", remap = false)
public abstract class WildlifeQuestGeladaClearingMixin {
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean jem$cleared(Level level, BlockPos pos, boolean drops) {
        var before = level.getBlockState(pos); boolean accepted = level.destroyBlock(pos, drops);
        if (accepted && before.getBlock() != level.getBlockState(pos).getBlock() && WildlifeQuestContext.current() instanceof EntityGeladaMonkey monkey && monkey.getClearGrassTime() > 0) {
            var actor = WildlifeQuestProof.actor(monkey, "worker_actor"); if (actor != null) WildlifeQuestProof.emit(actor, "v3_fauna_animal_work");
        }
        return accepted;
    }
}
