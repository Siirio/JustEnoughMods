package com.justenoughmods.achievementguide.integration;

import com.siirio.jemdrill.api.DrillLifecycleEvent;
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
        var player = event.getPlayer();
        var advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("jem", "engineering/" + key));
        if (advancement != null) {
            var progress = player.getAdvancements().getOrStartProgress(advancement);
            progress.getRemainingCriteria().forEach(criterion -> player.getAdvancements().award(advancement, criterion));
        }
    }
}
