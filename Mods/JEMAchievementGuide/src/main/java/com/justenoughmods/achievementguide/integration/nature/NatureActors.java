package com.justenoughmods.achievementguide.integration.nature;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import net.minecraft.server.level.ServerPlayer;

/** Reuses the existing generic read-only authenticated survival admission. */
public final class NatureActors {
    private NatureActors() {}
    public static boolean eligible(ServerPlayer actor) { return TravelProof.eligible(actor); }
    public static void emit(ServerPlayer actor, String action) {
        if (eligible(actor)) QuestActionTrigger.INSTANCE.emit(actor, action);
    }
}
