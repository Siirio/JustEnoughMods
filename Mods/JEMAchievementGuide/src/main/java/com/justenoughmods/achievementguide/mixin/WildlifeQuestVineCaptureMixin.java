package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityVineLasso", remap = false)
public abstract class WildlifeQuestVineCaptureMixin {
    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/util/VineLassoUtil;lassoTo(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void jem$bound(LivingEntity controller, LivingEntity target) {
        boolean before = VineLassoUtil.hasLassoData(target); VineLassoUtil.lassoTo(controller, target);
        var actor = controller instanceof Player player ? WildlifeQuestProof.real(player) : null;
        if (!before && actor != null && WildlifeQuestProof.hostile(target, actor) && VineLassoUtil.hasLassoData(target) && VineLassoUtil.getLassoedTo(target) == actor) {
            WildlifeQuestProof.mark(target, "vine_native_actor", actor.getUUID()); WildlifeQuestProof.data(target).putUUID("vine_native_source", target.getUUID());
        }
    }
}
