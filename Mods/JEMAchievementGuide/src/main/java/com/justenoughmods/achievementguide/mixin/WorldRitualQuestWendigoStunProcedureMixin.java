package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestStun;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=net.imasillylittleguy.cnc.procedures.WendigoStunProcedure.class,remap=false)
public abstract class WorldRitualQuestWendigoStunProcedureMixin {
    @Inject(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"))
    private static void jem$before(Event event,LevelAccessor world,double x,double y,double z,DamageSource source,Entity entity,CallbackInfo ci){WorldRitualQuestStun.begin(event,entity,source);}
    @Inject(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/entity/Entity;)V",at=@At("RETURN"))
    private static void jem$after(Event event,LevelAccessor world,double x,double y,double z,DamageSource source,Entity entity,CallbackInfo ci){WorldRitualQuestStun.end();}
}
