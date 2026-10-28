package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.BubbleArrowEntity", remap = false)
public abstract class BubbleArrowQuestMixin {
    @Inject(method = "m_8119_", at = @At(value = "INVOKE",
            target = "Lnet/sweenus/simplybows/entity/BubbleArrowEntity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void jem$nativeWaterDragCompensated(CallbackInfo callback) {
        Projectile projectile = (Projectile) (Object) this;
        var proof = projectile.getPersistentData();
        if (proof.getBoolean("JemQuestBubbleUnderwaterV4") && projectile.getDeltaMovement().lengthSqr() > 0
                && proof.getString("JemQuestNativeBowSubjectV4").equals("simplybows:bubble_bow/bubble_bow")
                && proof.hasUUID("JemQuestNativeBowActorV4") && projectile.getOwner() instanceof ServerPlayer actor
                && actor.getUUID().equals(proof.getUUID("JemQuestNativeBowActorV4"))) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_bow_bubble");
            proof.remove("JemQuestBubbleUnderwaterV4");
        }
    }
}
