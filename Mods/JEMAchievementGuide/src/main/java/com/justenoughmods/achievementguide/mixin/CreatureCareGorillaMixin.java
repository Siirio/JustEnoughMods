package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityGorilla", remap = false)
public abstract class CreatureCareGorillaMixin {
    @Shadow private UUID bananaThrowerID;
    @Unique private ItemStack jem$food = ItemStack.EMPTY;
    @Unique private boolean jem$wild;
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) {
        var self = (TamableAnimal)(Object)this;
        jem$wild = !self.isTame(); jem$food = self.getMainHandItem().copy();
    }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$friend(CallbackInfo callback) {
        var self = (TamableAnimal)(Object)this;
        if (jem$wild && self.isTame() && bananaThrowerID != null && bananaThrowerID.equals(self.getOwnerUUID())
                && CreatureCareProof.item(jem$food).equals("alexsmobs:banana")
                && CreatureCareProof.debited(jem$food, self.getMainHandItem())) {
            CreatureCareProof.mark(self, "gorilla_food_owner", bananaThrowerID);
        }
        jem$food = ItemStack.EMPTY;
    }
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$defense(LivingEntity target, DamageSource source, float amount) {
        float health = target.getHealth(); boolean accepted = target.hurt(source, amount);
        CreatureCareProof.defended((TamableAnimal)(Object)this, target, health, accepted, "gorilla_food_owner");
        return accepted;
    }
}
