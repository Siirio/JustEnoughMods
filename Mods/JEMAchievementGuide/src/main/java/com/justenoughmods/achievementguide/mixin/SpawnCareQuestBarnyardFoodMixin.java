package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = {"net.mcreator.barnyardbuddies.procedures.GreatPyreneesRightClickedOnEntityProcedure", "net.mcreator.barnyardbuddies.procedures.ManxCatRightClickedOnEntityProcedure"}, remap = false)
public abstract class SpawnCareQuestBarnyardFoodMixin {
    @Unique private static final ThreadLocal<SpawnCareQuestProof.Interaction> jem$feed = new ThreadLocal<>();
    @Inject(method = "execute", at = @At("HEAD"))
    private static void jem$before(LevelAccessor level, Entity entity, Entity source, CallbackInfo callback) {
        jem$feed.remove(); if (entity instanceof LivingEntity living && source instanceof Player player && SpawnCareQuestProof.item(player.getMainHandItem()).equals("barnyardbuddies:pet_kibble") && living.getHealth() < living.getMaxHealth()) jem$feed.set(SpawnCareQuestProof.begin(living, player, InteractionHand.MAIN_HAND));
    }
    @Inject(method = "execute", at = @At("RETURN"))
    private static void jem$after(LevelAccessor level, Entity entity, Entity source, CallbackInfo callback) {
        var run = jem$feed.get(); jem$feed.remove();
        if (run != null && run.owned() && entity instanceof TamableAnimal tame && tame.isOwnedBy(run.actor()) && run.entity().getHealth() > run.health()) SpawnCareQuestProof.fed(run);
    }
}
