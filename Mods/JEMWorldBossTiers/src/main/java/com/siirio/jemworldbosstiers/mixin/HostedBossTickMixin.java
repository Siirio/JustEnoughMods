package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class HostedBossTickMixin {
    @Redirect(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void jem$nativeCombatSource(Entity entity) {
        com.siirio.jemworldbosstiers.encounter.NativeEncounterSource.run(entity, entity::tick);
    }

    // NoAI stops native goals, but custom boss tick overrides also run combat and presentation.
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jem$waitForHostedStart(Entity entity, CallbackInfo callback) {
        if (com.siirio.jemworldbosstiers.revival.NativeBossStorage.pending(entity)) {
            callback.cancel();
            return;
        }
        Entity owner = HostedEncounterApi.damageOwner(entity);
        if (owner instanceof LivingEntity boss && HostedEncounterApi.held(boss)) {
            if (entity == boss) HostedEncounterApi.hold(boss);
            // Transient native effects must age out while their held owner's damage is denied.
            if (entity instanceof LivingEntity) callback.cancel();
        }
    }
}
