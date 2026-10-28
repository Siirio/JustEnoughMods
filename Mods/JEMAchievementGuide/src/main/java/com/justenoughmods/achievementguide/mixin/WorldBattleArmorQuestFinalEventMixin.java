package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraftforge.eventbus.api.Event;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.minecraftforge.eventbus.EventBus",remap=false,priority=500) public abstract class WorldBattleArmorQuestFinalEventMixin {
 @Inject(method="post(Lnet/minecraftforge/eventbus/api/Event;)Z",at=@At("RETURN"),remap=false) private void jem$final(Event event,CallbackInfoReturnable<Boolean> ci){WorldBattleArmorQuestLife.posted(event);}
}
