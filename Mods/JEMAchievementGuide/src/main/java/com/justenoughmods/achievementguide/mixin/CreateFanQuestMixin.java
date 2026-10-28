package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.fan.processing.FanProcessing", remap = false)
public abstract class CreateFanQuestMixin {
    @Unique private static final ThreadLocal<UUID> jem$actor = new ThreadLocal<>();

    @Inject(method = "applyProcessing(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z",
            at = @At("HEAD"), remap = false)
    private static void jem$begin(ItemEntity input, @Coerce Object type, CallbackInfoReturnable<Boolean> callback) {
        jem$actor.set(CreateRunProofs.actor(input));
    }

    @Inject(method = "applyProcessing(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z",
            at = @At("RETURN"), remap = false)
    private static void jem$washed(ItemEntity input, @Coerce Object type, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && !input.getItem().isEmpty()
                && type.getClass().getName().equals("com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes$SplashingType")) {
            CreateRunProofs.entitySuccess(input, jem$actor.get(), "create_fan_wash");
        }
        jem$actor.remove();
    }
}
