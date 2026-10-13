package com.siirio.jemprofessions.compat.irons;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.api.ProfessionProgressRequest;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.SourceKind;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class IronsProfessionAdapter {
    private IronsProfessionAdapter() {
    }

    @SubscribeEvent
    public static void spellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        ResourceLocation spell = ResourceLocation.tryParse(event.getSpellId());
        if (spell == null || !"irons_spellbooks".equals(spell.getNamespace())) return;
        String transaction = player.getUUID() + ":" + player.serverLevel().getGameTime() + ":" + spell + ":" + event.getCastSource();
        UUID auditId = UUID.nameUUIDFromBytes(transaction.getBytes(StandardCharsets.UTF_8));
        ProfessionApi.credit(player, new ProfessionProgressRequest(ProfessionId.MAGE, SourceKind.SPELL_CAST,
                spell, Math.max(1, event.getSpellLevel()), auditId));
    }
}
