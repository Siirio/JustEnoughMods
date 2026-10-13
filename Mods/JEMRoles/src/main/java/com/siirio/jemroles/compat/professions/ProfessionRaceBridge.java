package com.siirio.jemroles.compat.professions;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.api.ProfessionProgressEvent;
import com.siirio.jemprofessions.api.ProfessionProgressResult;
import com.siirio.jemroles.race.RaceService;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ProfessionRaceBridge {
    private ProfessionRaceBridge() {}

    @SubscribeEvent
    public static void acceptedProgress(ProfessionProgressEvent event) {
        if (event.result().status() != ProfessionProgressResult.Status.ACCEPTED) return;
        int bonus = RaceService.reserveProfessionBonus(event.player(), event.request().baseDelta());
        if (bonus > 0) ProfessionApi.administrativeCredit(event.player(), event.request().profession(), bonus);
    }
}
