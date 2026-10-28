package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.mcreator.unearthedjourney.UnearthedJourneyMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="net.mcreator.unearthedjourney.procedures.LeptictidiumRightClickedOnEntityProcedure",remap=false)
public abstract class WorldPrehistoricQuestLeptiMixin {
 @Inject(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"))private static void jem$before(Event event,LevelAccessor world,Entity entity,Entity actor,CallbackInfo ci){WorldPrehistoricQuestAsync.beginDig(entity,actor);}
 @Inject(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",at=@At("RETURN"))private static void jem$after(Event event,LevelAccessor world,Entity entity,Entity actor,CallbackInfo ci){WorldPrehistoricQuestAsync.finishDig();}
 @Redirect(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",at=@At(value="INVOKE",target="Lnet/mcreator/unearthedjourney/UnearthedJourneyMod;queueServerWork(ILjava/lang/Runnable;)V"))private static void jem$nativeQueue(int delay,Runnable work){UnearthedJourneyMod.queueServerWork(delay,WorldPrehistoricQuestAsync.nativeWork(work));}
}
