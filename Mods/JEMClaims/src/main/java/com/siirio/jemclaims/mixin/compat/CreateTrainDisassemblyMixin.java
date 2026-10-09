package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.entity.Train", remap = false)
public abstract class CreateTrainDisassemblyMixin {
    @Unique private static final int JEM_STATION_OFFSET = 1;
    @Unique private static final int JEM_INVERTED_BOGEY_OFFSET = 2;
    @Shadow public List<Carriage> carriages;
    @Shadow public List<Integer> carriageSpacing;
    @Shadow public boolean currentlyBackwards;

    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void jemClaims$train(Direction direction, BlockPos station, CallbackInfoReturnable<Boolean> cir) {
        int offset = JEM_STATION_OFFSET;
        for (int index = 0; index < carriages.size(); index++) {
            Carriage carriage = carriages.get(currentlyBackwards ? carriages.size() - index - 1 : index);
            CarriageContraptionEntity entity = carriage.anyAvailableEntity();
            if (entity == null || entity.getContraption() == null || !(entity.level() instanceof ServerLevel level)) {
                cir.setReturnValue(false);
                return;
            }
            StructureTransform transform = ((ContraptionTransformAccessor) entity).jemClaims$transform();
            transform.offset = station.relative(direction, offset + (currentlyBackwards ? carriage.bogeySpacing : 0))
                    .below(carriage.leadingBogey().isUpsideDown() ? JEM_INVERTED_BOGEY_OFFSET : 0);
            if (entity.getContraption().getBlocks().keySet().stream().map(transform::apply).anyMatch(target ->
                    !FlanBridge.canAutomate(level, entity.getContraption().anchor, target, ClaimPermission.PLACE)
                            || !FlanBridge.canAutomate(level, entity.getContraption().anchor, target, ClaimPermission.BREAK))) {
                cir.setReturnValue(false);
                return;
            }
            entity.getContraption().getBlocks().keySet().forEach(position -> com.siirio.jemclaims.compat.CreatePlacement.placing(level, entity.getContraption().anchor, transform.apply(position)));
            offset += carriage.bogeySpacing;
            if (index < carriageSpacing.size()) offset += carriageSpacing.get(currentlyBackwards ? carriageSpacing.size() - index - 1 : index);
        }
    }
}
