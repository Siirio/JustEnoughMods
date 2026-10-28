package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity", remap = false)
public abstract class CreatePressQuestMixin {
    @Unique private UUID jem$inputActor;
    @Unique private ItemEntity jem$inputEntity;

    @Inject(method = "tryProcessInWorld", at = @At("HEAD"), remap = false)
    private void jem$begin(ItemEntity entity, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        jem$inputEntity = simulate ? null : entity;
        jem$inputActor = simulate ? null : CreateRunProofs.actor(entity);
    }

    @Inject(method = "onItemPressed", at = @At("TAIL"), remap = false)
    private void jem$pressed(ItemStack output, CallbackInfo callback) {
        if (jem$inputEntity != null && CreateRunProofs.item(output, "create:iron_sheet")) {
            CreateRunProofs.entitySuccess(jem$inputEntity, jem$inputActor, "create_press_iron_sheet");
        }
    }

    @Inject(method = "tryProcessInWorld", at = @At("RETURN"), remap = false)
    private void jem$finish(ItemEntity entity, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        jem$inputActor = null;
        jem$inputEntity = null;
    }
}
