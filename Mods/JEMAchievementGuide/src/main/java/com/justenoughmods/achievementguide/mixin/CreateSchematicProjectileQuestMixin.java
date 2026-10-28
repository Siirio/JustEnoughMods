package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateSchematicProjectile;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import java.util.UUID;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.schematics.cannon.LaunchedItem", remap = false)
public abstract class CreateSchematicProjectileQuestMixin implements CreateSchematicProjectile {
    @Unique private UUID jem$nativeJob;
    @Override public UUID jem$job() { return jem$nativeJob; }
    @Override public void jem$job(UUID job) { jem$nativeJob = job; }

    @Inject(method = "serializeNBT", at = @At("RETURN"), remap = false)
    private void jem$save(CallbackInfoReturnable<CompoundTag> callback) {
        if (jem$nativeJob != null) callback.getReturnValue().putUUID("jemQuestCannonJobV1", jem$nativeJob);
    }

    @Inject(method = "fromNBT", at = @At("RETURN"), remap = false)
    private static void jem$load(CompoundTag tag, HolderGetter<Block> blocks, CallbackInfoReturnable<LaunchedItem> callback) {
        if (tag.hasUUID("jemQuestCannonJobV1")) {
            ((CreateSchematicProjectile)callback.getReturnValue()).jem$job(tag.getUUID("jemQuestCannonJobV1"));
        }
    }
}
