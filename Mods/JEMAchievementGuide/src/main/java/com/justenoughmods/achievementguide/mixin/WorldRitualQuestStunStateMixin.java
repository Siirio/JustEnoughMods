package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestStun;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SynchedEntityData.class)
public abstract class WorldRitualQuestStunStateMixin {
    @Shadow @Final private Entity entity;
    @Inject(method="set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",at=@At("RETURN"))
    private <T> void jem$state(EntityDataAccessor<T> key,T value,boolean force,CallbackInfo ci){WorldRitualQuestStun.state(entity,key,value);}
}
