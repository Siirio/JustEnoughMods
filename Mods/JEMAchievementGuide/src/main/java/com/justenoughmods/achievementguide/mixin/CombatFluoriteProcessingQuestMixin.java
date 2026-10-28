package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRegrowthProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.mcreator.deepdarkregrowth.procedures.WhenRightClikFlCrystalProcedure", remap = false)
public abstract class CombatFluoriteProcessingQuestMixin {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private static void jem$begin(Event event, LevelAccessor level, double x, double y, double z, Entity actor, CallbackInfo callback) { CombatRegrowthProof.begin("fluorite", level, event == null ? null : actor, event, x, y, z); }
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private static void jem$end(Event event, LevelAccessor level, double x, double y, double z, Entity actor, CallbackInfo callback) { CombatRegrowthProof.end(); }
}
