package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateFunnelRun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {"com.simibubi.create.content.logistics.funnel.FunnelBlock", "com.simibubi.create.content.logistics.funnel.BeltFunnelBlock"}, remap = false)
public abstract class CreateFunnelConfigurationQuestMixin {
    @Inject(method = "onWrenched", at = @At("RETURN"), remap = false)
    private void jem$nativeConfiguration(BlockState oldState, UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (context.getPlayer() instanceof ServerPlayer actor && callback.getReturnValue().consumesAction()
                && !oldState.equals(context.getLevel().getBlockState(context.getClickedPos()))
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof CreateFunnelRun funnel) {
            funnel.jem$beltConfiguration(actor);
        }
    }
}
