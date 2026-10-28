package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestStun;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.minecraftforge.eventbus.EventBus",remap=false,priority=500)
public abstract class WorldRitualQuestFinalEventMixin {
    @Inject(method="post(Lnet/minecraftforge/eventbus/api/Event;)Z",at=@At("RETURN"))private void jem$posted(Event event,CallbackInfoReturnable<Boolean> ci){WorldRitualQuestStun.posted(event);}
}
