package com.siirio.jemworldbosstiers.compat.irons;

import com.siirio.jemworldbosstiers.api.WorldTierApi;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class IronsSpellDamageAdapter {
    private IronsSpellDamageAdapter() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void scaleRegisteredSpellDamage(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSpellDamageSource().getEntity() instanceof ServerPlayer caster)) return;
        if (event.getEntity() instanceof ServerPlayer) return;
        if (IronsSpellCatalog.registeredId(event.getSpellDamageSource().spell()).isEmpty()) return;
        event.setAmount(event.getAmount() * SpellTierFactors.at(WorldTierApi.currentTier(caster.server)));
    }
}
