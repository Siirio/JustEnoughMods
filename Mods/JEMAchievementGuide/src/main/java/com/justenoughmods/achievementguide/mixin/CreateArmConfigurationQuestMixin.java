package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateArmRun;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket", remap = false)
public abstract class CreateArmConfigurationQuestMixin {
    @Shadow private ListTag receivedTag;
    @Shadow private BlockPos pos;

    @Inject(method = "lambda$handle$1", at = @At("TAIL"), remap = false)
    private void jem$committedConfiguration(NetworkEvent.Context context, CallbackInfo callback) {
        var actor = context.getSender();
        if (actor != null && actor.serverLevel().hasChunkAt(pos)
                && actor.serverLevel().getBlockEntity(pos) instanceof CreateArmRun arm) {
            arm.jem$configured(actor, receivedTag);
        }
    }
}
