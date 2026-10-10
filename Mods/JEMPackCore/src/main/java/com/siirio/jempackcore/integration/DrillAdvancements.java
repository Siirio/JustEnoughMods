package com.siirio.jempackcore.integration;

import com.siirio.jemdrill.api.DrillLifecycleEvent;
import com.siirio.jemtwelveeyes.AdvancementAwards;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DrillAdvancements {
    private DrillAdvancements() {
    }

    @SubscribeEvent
    public static void lifecycle(DrillLifecycleEvent event) {
        switch (event.getAction()) {
            case WORK -> award(event, "drill_work");
            case REPLACED -> award(event, "drill_replaced");
            case SERVICED -> {
                award(event, "drill_service");
                if (event.isMovingContraption() && event.getServicedCount() > 1) award(event, "batch_service");
                if (event.isMovingContraption() && event.isFinalService()) award(event, "drill_last_service");
            }
        }
    }

    private static void award(DrillLifecycleEvent event, String key) {
        AdvancementAwards.award(event.getPlayer(), new ResourceLocation("jem", "engineering/" + key));
    }
}
