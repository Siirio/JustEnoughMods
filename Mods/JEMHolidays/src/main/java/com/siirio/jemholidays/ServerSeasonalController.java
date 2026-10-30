package com.siirio.jemholidays;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Mod.EventBusSubscriber(modid = JEMHolidays.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerSeasonalController {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MIDNIGHT = 18_000L;

    private static LocalDate observedDate;
    private static boolean halloweenActive;
    private static long nextDateBoundaryMillis;
    private static long nextZoneCheckNanos;

    private ServerSeasonalController() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        if (observedDate == null || nowMillis >= nextDateBoundaryMillis || nowNanos >= nextZoneCheckNanos) {
            observeCalendar(nowMillis, nowNanos);
        }
        if (halloweenActive) {
            keepSkyLevelsAtMidnight(event.getServer());
        }
    }

    private static void observeCalendar(long nowMillis, long nowNanos) {
        ZoneId currentZone = ZoneId.systemDefault();
        LocalDate currentDate = Instant.ofEpochMilli(nowMillis).atZone(currentZone).toLocalDate();
        boolean active = SeasonCalendar.isHalloween(currentDate);
        if (observedDate == null || active != halloweenActive) {
            halloweenActive = active;
            LOGGER.info("JEM Holidays server calendar state: date={}, zone={}, halloweenNight={}",
                    currentDate, currentZone, active);
        }
        observedDate = currentDate;
        nextDateBoundaryMillis = currentDate.plusDays(1).atStartOfDay(currentZone)
                .plusSeconds(2).toInstant().toEpochMilli();
        nextZoneCheckNanos = nowNanos + 15L * 60L * 1_000_000_000L;
    }

    private static void keepSkyLevelsAtMidnight(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.dimensionType().hasSkyLight() || level.dimensionType().fixedTime().isPresent()) {
                continue;
            }
            long day = Math.floorDiv(level.getDayTime(), 24_000L);
            long midnight = day * 24_000L + MIDNIGHT;
            if (level.getDayTime() != midnight) {
                level.setDayTime(midnight);
            }
        }
    }
}
