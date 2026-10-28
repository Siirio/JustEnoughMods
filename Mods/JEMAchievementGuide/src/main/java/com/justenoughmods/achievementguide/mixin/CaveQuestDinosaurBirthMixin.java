package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestNuclearHatch;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.DinosaurEggBlock.class,remap=false)
public abstract class CaveQuestDinosaurBirthMixin {
    @Redirect(method="spawnDinosaurs",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeHatchling(Level level,Entity baby){
        boolean accepted=level.addFreshEntity(baby);var origin=CaveQuestNuclearHatch.current();
        if(accepted && origin!=null && baby instanceof com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity && !level.getBlockState(origin.egg()).is(com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry.TREMORZILLA_EGG.get())){
            baby.getPersistentData().putUUID("jemCaveNuclearBirthV4",origin.explosion());baby.getPersistentData().putUUID("jemCaveHatchlingV4",baby.getUUID());
        }
        return accepted;
    }
}
