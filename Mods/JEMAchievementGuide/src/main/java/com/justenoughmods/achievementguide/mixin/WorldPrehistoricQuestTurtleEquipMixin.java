package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="net.mcreator.unearthedjourney.procedures.CarbonemysRightClickedOnEntityProcedure",remap=false)
public abstract class WorldPrehistoricQuestTurtleEquipMixin {
 @Inject(method="execute",at=@At("HEAD"))private static void jem$before(LevelAccessor world,Entity target,Entity actor,CallbackInfo ci){WorldPrehistoricQuestTurtle.equipBefore(target,actor);}
 @Inject(method="execute",at=@At("RETURN"))private static void jem$after(LevelAccessor world,Entity target,Entity actor,CallbackInfo ci){WorldPrehistoricQuestTurtle.equipAfter();}
}
