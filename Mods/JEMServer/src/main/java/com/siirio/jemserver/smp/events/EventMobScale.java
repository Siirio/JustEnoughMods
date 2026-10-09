package com.siirio.jemserver.smp.events;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class EventMobScale {
    public static final String KEY="jem:event_scale";
    public static final float MAX_SCALE=20;

    public static boolean supported(float scale) {
        return Float.isFinite(scale)&&scale>=1&&scale<=MAX_SCALE;
    }

    public static void apply(Entity entity,float scale) {
        if(!supported(scale)) throw new IllegalArgumentException("Unsupported encounter scale: "+scale);
        entity.getPersistentData().putFloat(KEY,scale);
        ScaleData data=ScaleTypes.BASE.getScaleData(entity);
        data.setScaleTickDelay(0);
        data.setScale(scale);
        data.setPersistence(true);
    }

    public static void clear(Entity entity) {
        entity.getPersistentData().remove(KEY);
        ScaleData data=ScaleTypes.BASE.getScaleData(entity);
        data.setScaleTickDelay(0);
        data.setScale(1);
        data.setPersistence(false);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player&&event.getTarget().getPersistentData().hasUUID(BloodMoon.EVENT_ID))
            EventNetwork.eventMob(player,event.getTarget());
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!event.getEntity().getPersistentData().contains(KEY)) return;
        event.getDrops().forEach(EventMobScale::clear);
    }

    private EventMobScale() {}
}
