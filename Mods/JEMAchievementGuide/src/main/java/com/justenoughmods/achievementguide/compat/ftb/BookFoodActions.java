package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.siirio.jemfood.FoodConsumptionEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BookFoodActions {
    private BookFoodActions() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookFoodActions.class); }

    @SubscribeEvent
    public static void consumed(FoodConsumptionEvent event) {
        if (event.admittedBundle()) {
            QuestActionTrigger.INSTANCE.emit(event.player(), "food/food_blessing_applied");
            if (event.previous().filter(previous -> !previous.equals(event.current())).isPresent()) {
                QuestActionTrigger.INSTANCE.emit(event.player(), "v3_food_meal_choice");
            }
        }
        if (event.varietyActive()) QuestActionTrigger.INSTANCE.emit(event.player(), "food/variety_bonus");
    }
}
