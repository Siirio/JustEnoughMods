package com.siirio.jemholidays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = JEMHolidays.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SeasonalController {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PACK_STATE_FILE = FMLPaths.CONFIGDIR.get().resolve("jem-holidays-pack-state.json");
    private static final String JALLOWEEN_PACK = "file/JEM Jalloween v0.1.zip";
    private static final String HALLOWEEN_GUI_PACK = "file/Halloween%20GUI%201.20.zip";
    private static final List<String> SEASONAL_PACKS = List.of(JALLOWEEN_PACK, HALLOWEEN_GUI_PACK);
    private static final String MENU_TRACK = "https://youtu.be/2PNX8arJLoU";
    private static final String[] WORLD_TRACKS = {
            "https://youtu.be/neUXoluDW1k",
            "https://youtu.be/EU5XFHhuLzY"
    };
    private static final AudioYtBridge AUDIO_YT = new AudioYtBridge();

    private static LocalDate observedDate;
    private static boolean halloweenActive;
    private static long nextDateBoundaryMillis;
    private static long nextZoneCheckNanos;
    private static boolean packReconcileRequested = true;
    private static long packReconcileNotBeforeMillis;
    private static MusicContext musicContext = MusicContext.NONE;
    private static int worldTrackIndex = Math.floorMod((int) System.nanoTime(), WORLD_TRACKS.length);
    private static long earliestTrackEndCheckMillis;
    private static int audioTick;

    private SeasonalController() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        if (observedDate == null || nowMillis >= nextDateBoundaryMillis || nowNanos >= nextZoneCheckNanos) {
            observeCalendar(nowMillis, nowNanos);
        }

        if (packReconcileRequested && nowMillis >= packReconcileNotBeforeMillis) {
            packReconcileRequested = false;
            reconcileSeasonalPacks(Minecraft.getInstance());
        }

        updateMusic(Minecraft.getInstance(), nowMillis);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof PackSelectionScreen) {
            packReconcileRequested = true;
            packReconcileNotBeforeMillis = System.currentTimeMillis() + 15_000L;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaySound(PlaySoundEvent event) {
        if (halloweenActive && event.getSound() != null
                && event.getSound().getSource() == SoundSource.MUSIC) {
            event.setSound(null);
        }
    }

    private static void observeCalendar(long nowMillis, long nowNanos) {
        ZoneId currentZone = ZoneId.systemDefault();
        LocalDate currentDate = Instant.ofEpochMilli(nowMillis).atZone(currentZone).toLocalDate();
        boolean active = SeasonCalendar.isHalloween(currentDate);
        if (observedDate == null || active != halloweenActive) {
            halloweenActive = active;
            packReconcileRequested = true;
            LOGGER.info("JEM Holidays calendar state: date={}, zone={}, halloween={}",
                    currentDate, currentZone, active);
        }
        observedDate = currentDate;
        nextDateBoundaryMillis = currentDate.plusDays(1).atStartOfDay(currentZone)
                .plusSeconds(2).toInstant().toEpochMilli();
        nextZoneCheckNanos = nowNanos + 15L * 60L * 1_000_000_000L;
    }

    private static void reconcileSeasonalPacks(Minecraft minecraft) {
        PackRepository repository = minecraft.getResourcePackRepository();
        repository.reload();
        List<String> current = new ArrayList<>(repository.getSelectedIds());
        List<String> target = halloweenActive
                ? enableSeasonalStack(repository, current)
                : restoreNormalStack(current);

        if (!target.equals(current)) {
            repository.setSelected(target);
            minecraft.options.updateResourcePacks(repository);
            LOGGER.info("JEM Holidays {} the reversible seasonal resource-pack stack",
                    halloweenActive ? "enabled" : "disabled");
        }
    }

    private static List<String> enableSeasonalStack(PackRepository repository, List<String> current) {
        SavedSelection saved = readSavedSelection();
        if (saved == null) {
            saved = new SavedSelection(new ArrayList<>(current));
            writeSavedSelection(saved);
        }

        List<String> target = withoutSeasonalPacks(current);
        for (String packId : SEASONAL_PACKS) {
            if (repository.getAvailableIds().contains(packId)) {
                target.add(packId);
            } else if (packId.equals(JALLOWEEN_PACK)) {
                LOGGER.error("JEM Holidays cannot enable missing required resource pack {}", packId);
            }
        }
        return target;
    }

    private static List<String> restoreNormalStack(List<String> current) {
        SavedSelection saved = readSavedSelection();
        if (saved == null) {
            return withoutSeasonalPacks(current);
        }

        List<String> target = withoutSeasonalPacks(current);
        for (String packId : SEASONAL_PACKS) {
            if (saved.selected.contains(packId)) {
                restoreNearSavedNeighbors(target, saved.selected, packId);
            }
        }
        deleteSavedSelection();
        return target;
    }

    private static List<String> withoutSeasonalPacks(List<String> selected) {
        List<String> result = new ArrayList<>(selected);
        result.removeIf(SEASONAL_PACKS::contains);
        return result;
    }

    private static void restoreNearSavedNeighbors(List<String> target, List<String> saved, String packId) {
        int savedIndex = saved.indexOf(packId);
        for (int index = savedIndex - 1; index >= 0; index--) {
            int targetIndex = target.indexOf(saved.get(index));
            if (targetIndex >= 0) {
                target.add(targetIndex + 1, packId);
                return;
            }
        }
        for (int index = savedIndex + 1; index < saved.size(); index++) {
            int targetIndex = target.indexOf(saved.get(index));
            if (targetIndex >= 0) {
                target.add(targetIndex, packId);
                return;
            }
        }
        target.add(packId);
    }

    private static SavedSelection readSavedSelection() {
        if (!Files.isRegularFile(PACK_STATE_FILE)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(PACK_STATE_FILE)) {
            SavedSelection saved = GSON.fromJson(reader, SavedSelection.class);
            return saved != null && saved.selected != null ? saved : null;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read the JEM Holidays resource-pack restoration state", exception);
            return null;
        }
    }

    private static void writeSavedSelection(SavedSelection saved) {
        try {
            Files.createDirectories(PACK_STATE_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(PACK_STATE_FILE)) {
                GSON.toJson(saved, writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save the pre-season resource-pack configuration", exception);
        }
    }

    private static void deleteSavedSelection() {
        try {
            Files.deleteIfExists(PACK_STATE_FILE);
        } catch (IOException exception) {
            LOGGER.warn("Could not remove the completed JEM Holidays restoration state", exception);
        }
    }

    private static void updateMusic(Minecraft minecraft, long nowMillis) {
        if (!halloweenActive || !AUDIO_YT.isAvailable()) {
            stopSeasonalMusic();
            AUDIO_YT.setSeasonalDurationOverride(false);
            return;
        }

        AUDIO_YT.setSeasonalDurationOverride(true);
        MusicContext desired = minecraft.level == null ? MusicContext.MENU : MusicContext.WORLD;
        float volume = seasonalMusicVolume(minecraft);
        if (desired != musicContext) {
            AUDIO_YT.stopOwned();
            musicContext = desired;
            minecraft.getMusicManager().stopPlaying();
            startCurrentTrack(nowMillis, volume);
        }

        AUDIO_YT.updateOwnedVolume(volume, musicContext == MusicContext.MENU);
        if (++audioTick % 20 != 0) {
            return;
        }

        if (nowMillis < earliestTrackEndCheckMillis || AUDIO_YT.isOwnedTrackPlaying()) {
            return;
        }

        if (musicContext == MusicContext.WORLD) {
            worldTrackIndex = (worldTrackIndex + 1) % WORLD_TRACKS.length;
        }
        startCurrentTrack(nowMillis, volume);
    }

    private static float seasonalMusicVolume(Minecraft minecraft) {
        return 0.55F
                * minecraft.options.getSoundSourceVolume(SoundSource.MUSIC)
                * minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
    }

    private static void startCurrentTrack(long nowMillis, float volume) {
        String url = musicContext == MusicContext.MENU
                ? MENU_TRACK
                : WORLD_TRACKS[worldTrackIndex];
        AUDIO_YT.playOwned(url, volume);
        earliestTrackEndCheckMillis = nowMillis + 10_000L;
    }

    private static void stopSeasonalMusic() {
        if (musicContext == MusicContext.NONE) {
            return;
        }
        AUDIO_YT.stopOwned();
        musicContext = MusicContext.NONE;
        earliestTrackEndCheckMillis = 0L;
        audioTick = 0;
    }

    private enum MusicContext {
        NONE,
        MENU,
        WORLD
    }

    private static final class SavedSelection {
        private List<String> selected;

        private SavedSelection(List<String> selected) {
            this.selected = selected;
        }
    }
}
