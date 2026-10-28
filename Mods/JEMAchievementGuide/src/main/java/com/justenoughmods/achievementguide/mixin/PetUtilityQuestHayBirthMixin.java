package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="tech.alexnijjar.golemoverhaul.common.utils.ModUtils",remap=false)
public abstract class PetUtilityQuestHayBirthMixin {
 @Redirect(method="spawnGolemInWorld",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private static boolean jem$constructed(Level l,Entity e){boolean accepted=l.addFreshEntity(e);if(accepted && PetUtilityQuestProof.id(e).equals("golemoverhaul:hay_golem"))PetUtilityQuestPlacement.accepted(e);return accepted;}
}
