package dev.taste.engine.mixin.client;

import dev.taste.engine.render.TasteRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
abstract class ClientLevelLightInvalidationMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void taste$invalidateLocalLights(BlockPos position, BlockState state, int flags, int recursionDepth,
            CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) TasteRenderer.instance().dirtyLocalLight((ClientLevel) (Object) this, position);
    }
}
