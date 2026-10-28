package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.event.ServerEvents", remap = false)
public abstract class WildlifeQuestVineReleaseMixin {
    @Redirect(method = "onInteractWithEntity", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/util/VineLassoUtil;lassoTo(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void jem$released(LivingEntity controller, LivingEntity target, EntityInteract event) {
        var actor = WildlifeQuestProof.real(event.getEntity());
        boolean owned = actor != null && VineLassoUtil.hasLassoData(target) && VineLassoUtil.getLassoedTo(target) == actor
                && WildlifeQuestProof.matches(target, "vine_native_actor", actor.getUUID())
                && WildlifeQuestProof.data(target).hasUUID("vine_native_source") && target.getUUID().equals(WildlifeQuestProof.data(target).getUUID("vine_native_source"));
        VineLassoUtil.lassoTo(controller, target);
        if (owned && controller == null && target.isAlive() && !VineLassoUtil.hasLassoData(target)) {
            WildlifeQuestProof.emit(actor, "v3_fauna_creature_transport"); WildlifeQuestProof.data(target).remove("vine_native_actor");
        }
    }
}
