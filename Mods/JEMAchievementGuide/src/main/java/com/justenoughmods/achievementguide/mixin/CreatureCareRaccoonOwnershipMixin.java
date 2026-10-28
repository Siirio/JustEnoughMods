package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareWash;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityRaccoon", remap = false)
public abstract class CreatureCareRaccoonOwnershipMixin {
    @Inject(method = "postWashItem", at = @At("HEAD"))
    private void jem$egg(ItemStack stack, CallbackInfo callback) { CreatureCareWash.washing((TamableAnimal)(Object)this, stack); }
}
