package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.Contraption", remap = false)
public abstract class CreateAssemblyMixin {
    @Shadow protected Map<BlockPos, StructureBlockInfo> blocks;
    @Shadow public BlockPos anchor;

    @Inject(method = "searchMovedStructure", at = @At("RETURN"), cancellable = true)
    private void jemClaims$assemble(Level level, BlockPos origin, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && level instanceof ServerLevel serverLevel
                && blocks.keySet().stream().anyMatch(local -> !FlanBridge.canAutomate(serverLevel, origin, local.offset(anchor), ClaimPermission.BREAK))) {
            cir.setReturnValue(false);
        }
    }
}
