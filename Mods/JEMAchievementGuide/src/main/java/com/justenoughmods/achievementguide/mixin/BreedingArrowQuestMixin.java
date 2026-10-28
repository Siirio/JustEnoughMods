package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.ArrowQuestActor;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.morearrows.specialarrowentities.padded.BreedingArrowEntity", remap = false)
public abstract class BreedingArrowQuestMixin {
    @Redirect(method = "applyBreedingEffectToSurrounding", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/animal/Animal;m_27595_(Lnet/minecraft/world/entity/player/Player;)V"))
    private void jem$actualLoveMode(Animal animal, Player cause) {
        boolean wasInLove = animal.isInLove();
        float health = animal.getHealth();
        animal.setInLove(cause);
        var actor = ArrowQuestActor.get((Projectile) (Object) this);
        if (actor != null && cause == actor && !wasInLove && animal.isAlive() && !animal.isBaby()
                && animal.isInLove() && animal.getLoveCause() == actor && animal.getHealth() == health) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_arrow_transport");
        }
    }
}
