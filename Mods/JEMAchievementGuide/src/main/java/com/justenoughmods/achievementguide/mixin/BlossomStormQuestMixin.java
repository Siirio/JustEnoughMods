package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BowEffectProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.world.BlossomStormManager", remap = false)
public abstract class BlossomStormQuestMixin {
    @Inject(method = "createStorm(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/sweenus/simplybows/upgrade/BowUpgradeData;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER))
    private static void jem$actualStormAdded(ServerLevel level, Vec3 center, LivingEntity target, Entity owner,
                                            @Coerce Object upgrades, CallbackInfo callback) {
        BowEffectProof.committed(owner, "simplybows:blossom_bow/blossom_bow", "v3_combat_bow_blossom");
    }
}
