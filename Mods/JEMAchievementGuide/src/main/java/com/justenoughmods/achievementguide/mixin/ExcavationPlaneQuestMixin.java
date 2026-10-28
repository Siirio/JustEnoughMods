package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "com.siirio.jemcompat.feature.excavation.ExcavationMining$Batch",
        "com.siirio.jemexcavation.feature.ExcavationMining$Batch"
}, remap = false)
public abstract class ExcavationPlaneQuestMixin {
    @Shadow @Final private ServerPlayer player;
    @Shadow private boolean removed;
    @Unique private int jem$removed;
    @Inject(method = "breakOne", at = @At("RETURN"), remap = false)
    private void jem$actualPlane(BlockPos target, CallbackInfoReturnable<Boolean> callback) {
        if (removed && callback.getReturnValueZ() && ++jem$removed == 2)
            QuestActionTrigger.INSTANCE.emit(player, "v3_eng_excavate");
    }
}
