package com.siirio.jemroles.compat.irons;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class IronsRaceBridge {
    private IronsRaceBridge() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void humanDirectHit(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSpellDamageSource().getEntity() instanceof ServerPlayer caster)) return;
        if (event.getEntity() instanceof ServerPlayer) return;
        var direct = event.getSpellDamageSource().getDirectEntity();
        if (direct != caster && !(direct instanceof Projectile)) return;
        var principal = IdentityPolicy.resolve(caster);
        if (principal.isEmpty() || RaceApi.effectiveRace(caster.server, principal.get().accountId()) != RaceId.HUMAN) return;
        event.setAmount(event.getAmount() + 1.0F);
    }
}
