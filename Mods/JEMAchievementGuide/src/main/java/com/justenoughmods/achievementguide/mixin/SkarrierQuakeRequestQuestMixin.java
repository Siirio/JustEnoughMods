package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.procedures.QuakeRidingAttackProcedure", remap = false)
public abstract class SkarrierQuakeRequestQuestMixin {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private static void jem$begin(Event event, Entity actor, CallbackInfo callback) {
        SkarrierWorkshopProof.begin("quake_request", actor, actor == null ? null : actor.getVehicle(), event);
    }
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private static void jem$end(Event event, Entity actor, CallbackInfo callback) { SkarrierWorkshopProof.end(); }
}
