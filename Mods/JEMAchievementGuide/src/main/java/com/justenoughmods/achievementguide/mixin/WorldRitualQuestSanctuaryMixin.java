package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestRitual;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=net.mcreator.luminousnether.procedures.SanctuaryTpProcedureProcedure.class,remap=false)
public abstract class WorldRitualQuestSanctuaryMixin {
    @Inject(method={"lambda$execute$0","lambda$execute$1","lambda$execute$2"},at=@At("HEAD"))private static void jem$before(Entity actor,CallbackInfo ci){WorldRitualQuestRitual.homeBegin(actor);}
    @Inject(method={"lambda$execute$0","lambda$execute$1","lambda$execute$2"},at=@At("RETURN"))private static void jem$after(Entity actor,CallbackInfo ci){WorldRitualQuestRitual.homeEnd(actor);}
}
