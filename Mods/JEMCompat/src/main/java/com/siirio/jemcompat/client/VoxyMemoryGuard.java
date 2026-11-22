package com.siirio.jemcompat.client;

import com.mojang.logging.LogUtils;
import com.siirio.jemcompat.JEMCompat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoxyMemoryGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SAMPLE_TICKS = 200L;
    private static final long MIB = 1024L * 1024L;
    private static long nextSampleTick;
    private static int allocationBand = -1;
    private static boolean configurationApplied;
    private static Method allocatedBytes;
    private static Method reclaimBuffers;

    private VoxyMemoryGuard() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!configurationApplied) {
            configurationApplied = true;
            applyCoherentConfiguration();
        }
        if (minecraft.level == null) {
            nextSampleTick = 0L;
            allocationBand = -1;
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        if (gameTime < nextSampleTick) return;
        nextSampleTick = gameTime + SAMPLE_TICKS;
        sampleNativeBuffers();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        nextSampleTick = 0L;
        allocationBand = -1;
        Minecraft.getInstance().execute(VoxyMemoryGuard::reclaimReleasedBuffers);
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        nextSampleTick = 0L;
        allocationBand = -1;
        Minecraft.getInstance().execute(VoxyMemoryGuard::reclaimReleasedBuffers);
    }

    private static void applyCoherentConfiguration() {
        if (!ModList.get().isLoaded("voxy")) return;
        try {
            Class<?> voxyType = Class.forName("me.cortex.voxy.client.config.VoxyConfig");
            Object voxy = voxyType.getField("CONFIG").get(null);
            boolean rendering = getBoolean(voxyType, voxy, "enabled")
                    && getBoolean(voxyType, voxy, "enableRendering");
            boolean voxyChanged = setBoolean(voxyType, voxy, "ingestEnabled", rendering);
            if (rendering) {
                voxyChanged |= setFloat(voxyType, voxy, "sectionRenderDistance", 32.0F);
                voxyChanged |= setInt(voxyType, voxy, "serviceThreads", 2);
                voxyChanged |= setBoolean(voxyType, voxy, "enableExtendedRequestDistance", false);
                voxyChanged |= setInt(voxyType, voxy, "requestDistance", 32);
            }
            if (voxyChanged) voxyType.getMethod("save").invoke(voxy);
            applyVssConfiguration(rendering);
        } catch (ReflectiveOperationException error) {
            LOGGER.warn("JEMCompat could not align the installed Neo Voxy configuration for this session", error);
        }
    }

    private static void applyVssConfiguration(boolean rendering) {
        if (!ModList.get().isLoaded("vss")) return;
        try {
            Class<?> vssType = Class.forName("dev.xantha.vss.config.VSSClientConfig");
            Object vss = vssType.getField("CONFIG").get(null);
            boolean changed = setBoolean(vssType, vss, "receiveServerLods", rendering);
            changed |= setInt(vssType, vss, "lodDistanceChunks", 32);
            changed |= setBoolean(vssType, vss, "enablePrediction", false);
            changed |= setBoolean(vssType, vss, "enableXaeroMapBridge", false);
            if (changed) vssType.getMethod("normalizeAndSave").invoke(vss);
        } catch (ReflectiveOperationException error) {
            LOGGER.warn("JEMCompat could not align the installed VSS client configuration for this session", error);
        }
    }

    private static boolean getBoolean(Class<?> type, Object owner, String name) throws ReflectiveOperationException {
        return type.getField(name).getBoolean(owner);
    }

    private static boolean setBoolean(Class<?> type, Object owner, String name, boolean value)
            throws ReflectiveOperationException {
        Field field = type.getField(name);
        if (field.getBoolean(owner) == value) return false;
        field.setBoolean(owner, value);
        return true;
    }

    private static boolean setInt(Class<?> type, Object owner, String name, int value)
            throws ReflectiveOperationException {
        Field field = type.getField(name);
        if (field.getInt(owner) == value) return false;
        field.setInt(owner, value);
        return true;
    }

    private static boolean setFloat(Class<?> type, Object owner, String name, float value)
            throws ReflectiveOperationException {
        Field field = type.getField(name);
        if (Float.compare(field.getFloat(owner), value) == 0) return false;
        field.setFloat(owner, value);
        return true;
    }

    private static void sampleNativeBuffers() {
        try {
            if (allocatedBytes == null) {
                Class<?> nativeBuffer = Class.forName("me.jellysquid.mods.sodium.client.util.NativeBuffer");
                allocatedBytes = nativeBuffer.getMethod("getTotalAllocated");
                reclaimBuffers = nativeBuffer.getMethod("reclaim", boolean.class);
            }
            long bytes = (long) allocatedBytes.invoke(null);
            int nextBand = bytes >= 2L * 1024L * MIB ? 3 : bytes >= 1024L * MIB ? 2 : bytes >= 512L * MIB ? 1 : 0;
            if (allocationBand >= 0 && nextBand != allocationBand) {
                LOGGER.info("Embeddium native buffers crossed into band {} at {} MiB", bandLabel(nextBand), bytes / MIB);
            }
            allocationBand = nextBand;
        } catch (ReflectiveOperationException error) {
            allocatedBytes = null;
            reclaimBuffers = null;
        }
    }

    private static String bandLabel(int band) {
        return switch (band) {
            case 0 -> "below 512 MiB";
            case 1 -> "512 MiB to 1 GiB";
            case 2 -> "1 GiB to 2 GiB";
            default -> "2 GiB or higher";
        };
    }

    public static void reclaimReleasedBuffers() {
        try {
            if (reclaimBuffers == null) sampleNativeBuffers();
            if (reclaimBuffers != null) reclaimBuffers.invoke(null, true);
        } catch (ReflectiveOperationException error) {
            reclaimBuffers = null;
        }
    }

    @Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ReloadEvents {
        private ReloadEvents() {
        }

        @SubscribeEvent
        public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> reclaimReleasedBuffers());
        }
    }
}
