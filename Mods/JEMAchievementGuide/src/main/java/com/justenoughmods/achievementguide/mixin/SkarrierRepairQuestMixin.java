package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SkarrierRepairProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.procedures.WorkshopOnButtonClickProcedure", remap = false)
public abstract class SkarrierRepairQuestMixin {
    @Inject(method = "execute", at = @At("HEAD"))
    private static void jem$beforeRepair(LevelAccessor world, double x, double y, double z, Entity user, CallbackInfo callback) {
        if (user instanceof ServerPlayer actor && actor.level() == world) SkarrierRepairProof.begin(actor);
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private static void jem$afterRepair(LevelAccessor world, double x, double y, double z, Entity user, CallbackInfo callback) {
        if (user instanceof ServerPlayer actor && actor.level() == world) SkarrierRepairProof.end(actor);
    }
}
