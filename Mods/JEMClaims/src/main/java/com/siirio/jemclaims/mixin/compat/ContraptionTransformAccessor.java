package com.siirio.jemclaims.mixin.compat;

import com.simibubi.create.content.contraptions.StructureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", remap = false)
public interface ContraptionTransformAccessor {
    @Invoker("makeStructureTransform")
    StructureTransform jemClaims$transform();
}
