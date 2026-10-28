package com.justenoughmods.achievementguide.mixin;
import net.mcreator.barnyardbuddies.entity.GreatPyreneesDogEntity;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.mcreator.barnyardbuddies.procedures.GreatPyreneesRightClickedOnEntityProcedure", remap = false)
public abstract class SpawnCareQuestVestMixin {
    @Unique private static final ThreadLocal<SpawnCareQuestProof.Interaction> jem$vest = new ThreadLocal<>();
    @Inject(method = "execute", at = @At("HEAD"))
    private static void jem$before(LevelAccessor level, Entity entity, Entity source, CallbackInfo callback) {
        jem$vest.remove();
        if (entity instanceof GreatPyreneesDogEntity dog && source instanceof Player player && player.isShiftKeyDown() && !dog.getEntityData().get(GreatPyreneesDogEntity.DATA_armored)
                && SpawnCareQuestProof.item(player.getMainHandItem()).equals("barnyardbuddies:pyrenees_protective_vest")) jem$vest.set(SpawnCareQuestProof.begin(dog, player, InteractionHand.MAIN_HAND));
    }
    @Inject(method = "execute", at = @At("RETURN"))
    private static void jem$after(LevelAccessor level, Entity entity, Entity source, CallbackInfo callback) {
        var run = jem$vest.get(); jem$vest.remove();
        if (run != null && run.owned() && entity instanceof GreatPyreneesDogEntity dog && dog.isOwnedBy(run.actor()) && dog.getEntityData().get(GreatPyreneesDogEntity.DATA_armored)) SpawnCareQuestProof.emit(run.actor(), "v3_pet_guardian_vest");
    }
}
