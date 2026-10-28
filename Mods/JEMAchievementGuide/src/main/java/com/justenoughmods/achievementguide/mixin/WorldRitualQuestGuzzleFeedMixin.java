package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.level.LevelAccessor;import net.minecraft.world.entity.Entity;
@Mixin(value=net.mcreator.luminousnether.procedures.GuzzleghastRightClickedOnEntityProcedure.class,remap=false)
public abstract class WorldRitualQuestGuzzleFeedMixin {
    @Inject(method="execute",at=@At("HEAD"))private static void jem$before(LevelAccessor world,double x,double y,double z,Entity creature,Entity actor,CallbackInfo ci){WorldRitualQuestEctoplasm.feed(creature,actor);}
    @Inject(method="execute",at=@At("RETURN"))private static void jem$after(LevelAccessor world,double x,double y,double z,Entity creature,Entity actor,CallbackInfo ci){WorldRitualQuestEctoplasm.end();}
}
