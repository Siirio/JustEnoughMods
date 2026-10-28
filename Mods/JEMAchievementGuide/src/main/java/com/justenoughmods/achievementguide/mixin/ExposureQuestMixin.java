package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.github.mortuusars.exposure.advancements.trigger.FrameExposedTrigger;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FrameExposedTrigger.class, remap = false)
public abstract class ExposureQuestMixin {
    @Inject(method = "trigger", at = @At("TAIL"))
    private void jem$frameExposed(ServerPlayer actor, CameraHolder holder, ItemStack camera, Frame frame,
                                  List<BlockPos> positions, List<LivingEntity> entities, CallbackInfo callback) {
        if (!camera.isEmpty() && !frame.identifier().isEmpty() && frame.isTakenBy(actor)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_home_first_frame");
        }
    }
}
