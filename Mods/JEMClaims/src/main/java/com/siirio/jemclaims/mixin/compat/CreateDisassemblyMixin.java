package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", remap = false)
public abstract class CreateDisassemblyMixin {
    @Shadow protected Contraption contraption;
    @Shadow protected abstract StructureTransform makeStructureTransform();

    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void jemClaims$disassemble(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (contraption == null || !(entity.level() instanceof ServerLevel level)) return;
        StructureTransform transform = makeStructureTransform();
        if (contraption.getBlocks().keySet().stream().map(transform::apply).anyMatch(target ->
                !FlanBridge.canAutomate(level, contraption.anchor, target, ClaimPermission.PLACE)
                        || !FlanBridge.canAutomate(level, contraption.anchor, target, ClaimPermission.BREAK))) { ci.cancel(); return; }
        contraption.getBlocks().keySet().forEach(position -> com.siirio.jemclaims.compat.CreatePlacement.placing(level, contraption.anchor, transform.apply(position)));
    }
}
