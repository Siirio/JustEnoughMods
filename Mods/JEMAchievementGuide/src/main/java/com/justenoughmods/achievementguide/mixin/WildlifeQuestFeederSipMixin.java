package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestContext;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestSetups;
import com.github.alexthe666.alexsmobs.entity.EntityHummingbird;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityHummingbird$AIUseFeeder", remap = false)
public abstract class WildlifeQuestFeederSipMixin {
    @Shadow private BlockPos localFeeder;
    @Unique private int jem$crops;
    @Unique private UUID jem$actor;
    @Inject(method = "m_8037_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) {
        jem$actor = null;
        if (WildlifeQuestContext.current() instanceof EntityHummingbird bird && bird.level() instanceof ServerLevel level && localFeeder != null
                && WildlifeQuestSetups.property(level.getBlockState(localFeeder), "contents") == 3) {
            jem$crops = bird.getCropsPollinated(); var proof = WildlifeQuestSetups.get(level).at(level, localFeeder);
            if (proof.hasUUID("actor") && proof.getInt("paid_contents") == 3) jem$actor = proof.getUUID("actor");
        }
    }
    @Inject(method = "m_8037_", at = @At("RETURN"))
    private void jem$sipped(CallbackInfo callback) {
        if (jem$actor != null && WildlifeQuestContext.current() instanceof EntityHummingbird bird && bird.getCropsPollinated() == jem$crops + 1 && bird.sipCooldown >= 120) WildlifeQuestProof.mark(bird, "pollinator_actor", jem$actor);
        jem$actor = null;
    }
}
