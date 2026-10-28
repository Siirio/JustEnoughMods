package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.siirio.jemsettlements.api.SettlementOperationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BookSettlementActions {
    private BookSettlementActions() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookSettlementActions.class); }

    @SubscribeEvent
    public static void committed(SettlementOperationEvent event) {
        String action = switch (event.kind()) {
            case TERRITORY_EXPANDED -> "v3_town_expand";
            case FOOD_STORAGE_ENABLED -> "v3_town_supply";
            case RESIDENT_PAT -> "v3_town_kind_gesture";
            case COMFORT_CONFIRMED -> "v3_town_comfort";
            case ROOF_AND_WORK_CONFIRMED -> "v3_town_roof_and_work";
            case HAPPINESS_INCOME_COMMITTED -> "v3_town_first_income";
            case RESIDENT_MEAL -> "v3_town_supper";
        };
        QuestActionTrigger.INSTANCE.emit(event.player(), action);
    }
}
