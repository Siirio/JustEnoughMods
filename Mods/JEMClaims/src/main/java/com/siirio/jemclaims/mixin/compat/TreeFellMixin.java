package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import ht.treechop.common.chop.FellDataImpl;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ht.treechop.common.chop.FellTreeResult", remap = false)
public abstract class TreeFellMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private FellDataImpl fellData;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void jemClaims$fell(BlockPos pos, ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Stream<BlockPos> targets = fellData.getTree().streamLogs();
        if (fellData.getBreakLeaves()) targets = Stream.concat(targets, fellData.getTree().streamLeaves());
        try (Stream<BlockPos> affected = targets) {
            if (affected.anyMatch(target -> !FlanBridge.can(player, serverLevel, target, ClaimPermission.BREAK))) ci.cancel();
        }
    }
}
