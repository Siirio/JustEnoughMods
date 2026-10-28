package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo @Mixin(targets="net.mcreator.barnyardbuddies.procedures.ClydesdaleSprintOnKeyPressedProcedure",remap=false) public abstract class SpawnCareQuestClydesdaleSprintMixin { 
 @Inject(method="execute",at=@At("RETURN"),remap=false) private static void jem$sprint(net.minecraft.world.level.LevelAccessor world,net.minecraft.world.entity.Entity entity,CallbackInfo ci){if(entity instanceof net.minecraft.world.entity.player.Player player){var actor=SpawnCareQuestProof.real(player);if(actor!=null&&actor.getVehicle() instanceof net.mcreator.barnyardbuddies.entity.ClydesdaleHorseEntity horse&&horse.isOwnedBy(actor)&&horse.getEntityData().get(net.mcreator.barnyardbuddies.entity.ClydesdaleHorseEntity.DATA_Sprint))SpawnCareQuestProof.mark(horse,"rider_input",actor.getUUID());}}
 }
