package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.siirio.jemclaims.compat.MachineContext;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.utility.BlockHelper", remap = false)
public abstract class CreateBlockBreakMixin {
    @Inject(method = "destroyBlockAs", at = @At("HEAD"), cancellable = true)
    private static void jemClaims$break(Level level, BlockPos target, Player player, ItemStack tool, float chance, Consumer<ItemStack> drops, CallbackInfo ci) {
        if (MachineContext.source() != null && level instanceof ServerLevel serverLevel
                && !FlanBridge.canAutomate(serverLevel, MachineContext.source(), target, ClaimPermission.BREAK)) ci.cancel();
    }
}
