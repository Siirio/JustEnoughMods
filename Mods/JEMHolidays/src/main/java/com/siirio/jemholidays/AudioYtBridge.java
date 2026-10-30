package com.siirio.jemholidays;

import SCP.audioyt_1_20_1.audio.AudioManager;
import SCP.audioyt_1_20_1.config.ModConfig;
import SCP.audioyt_1_20_1.network.ModPackets.PlaySoundPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import javax.sound.sampled.FloatControl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class AudioYtBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String AUDIO_YT_MOD_ID = "audioyt_1_20_1";
    private static final float VOLUME_EPSILON = 0.005F;

    private AudioManager manager;
    private Field activeSounds;
    private Field running;
    private Field payload;
    private Field payloadVolume;
    private Field volumeControl;
    private Method setVolumeLinear;
    private UUID ownedSoundId;
    private boolean initializationAttempted;
    private int originalMaxDurationSec;
    private boolean durationLimitOverridden;
    private float lastPayloadVolume = Float.NaN;
    private float lastMenuVolume = Float.NaN;

    boolean isAvailable() {
        return initialize();
    }

    void setSeasonalDurationOverride(boolean enabled) {
        if (!enabled && durationLimitOverridden) {
            ModConfig.INSTANCE.maxDurationSec = originalMaxDurationSec;
            durationLimitOverridden = false;
            LOGGER.info("JEM Holidays restored AudioYT's configured duration limit");
            return;
        }
        if (!initialize()) {
            return;
        }
        if (enabled && !durationLimitOverridden) {
            originalMaxDurationSec = ModConfig.INSTANCE.maxDurationSec;
            ModConfig.INSTANCE.maxDurationSec = 0;
            durationLimitOverridden = true;
            LOGGER.info("JEM Holidays temporarily disabled AudioYT's duration limit for the Halloween playlist");
        }
    }

    void playOwned(String url, float volume) {
        if (!initialize()) {
            return;
        }
        stopOwned();
        ownedSoundId = UUID.randomUUID();
        lastPayloadVolume = clampVolume(volume);
        lastMenuVolume = Float.NaN;
        manager.play(new PlaySoundPayload(
                ownedSoundId,
                url,
                lastPayloadVolume,
                Optional.empty(),
                Optional.empty(),
                32.0F
        ));
    }

    void stopOwned() {
        if (ownedSoundId == null) {
            return;
        }
        if (initialize()) {
            manager.stop(ownedSoundId);
        }
        ownedSoundId = null;
        lastPayloadVolume = Float.NaN;
        lastMenuVolume = Float.NaN;
    }

    boolean isOwnedTrackPlaying() {
        Object sound = ownedSound();
        if (sound == null) {
            return false;
        }
        try {
            return running.getBoolean(sound);
        } catch (IllegalAccessException exception) {
            disable("Could not inspect JEM Holidays AudioYT playback", exception);
            return false;
        }
    }

    boolean hasAudibleExternalPositionalSound(Minecraft minecraft) {
        if (!initialize() || minecraft.player == null
                || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= VOLUME_EPSILON
                || minecraft.options.getSoundSourceVolume(SoundSource.RECORDS) <= VOLUME_EPSILON) {
            return false;
        }

        try {
            Vec3 listener = minecraft.player.getEyePosition(1.0F);
            float categoryVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER)
                    * minecraft.options.getSoundSourceVolume(SoundSource.RECORDS);
            for (Map.Entry<UUID, Object> entry : sounds().entrySet()) {
                if (entry.getKey().equals(ownedSoundId) || !running.getBoolean(entry.getValue())) {
                    continue;
                }

                Object soundPayload = payload.get(entry.getValue());
                if (!(soundPayload instanceof PlaySoundPayload external)
                        || external.position().isEmpty() || external.radius() <= 0.0F) {
                    continue;
                }

                double distance = listener.distanceTo(external.position().get());
                if (distance >= external.radius()) {
                    continue;
                }

                double attenuation = 1.0D - distance / external.radius();
                if (external.volume() * categoryVolume * attenuation > VOLUME_EPSILON) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException exception) {
            disable("Could not inspect external AudioYT playback", exception);
            return false;
        }
    }

    void updateOwnedVolume(float volume, boolean mainMenu) {
        Object sound = ownedSound();
        if (sound == null) {
            return;
        }
        float target = clampVolume(volume);
        try {
            Object soundPayload = payload.get(sound);
            if (Float.isNaN(lastPayloadVolume) || Math.abs(lastPayloadVolume - target) >= VOLUME_EPSILON) {
                payloadVolume.setFloat(soundPayload, target);
                lastPayloadVolume = target;
            }

            if (!mainMenu || (!Float.isNaN(lastMenuVolume)
                    && Math.abs(lastMenuVolume - target) < VOLUME_EPSILON)) {
                return;
            }
            if (volumeControl.get(sound) instanceof FloatControl) {
                setVolumeLinear.invoke(sound, target);
                lastMenuVolume = target;
            }
        } catch (ReflectiveOperationException exception) {
            disable("Could not update the JEM Holidays AudioYT volume", exception);
        }
    }

    private boolean initialize() {
        if (manager != null) {
            return true;
        }
        if (initializationAttempted) {
            return false;
        }
        initializationAttempted = true;
        if (!ModList.get().isLoaded(AUDIO_YT_MOD_ID)) {
            LOGGER.error("JEM Holidays requires AudioYT 1.0.1 for Halloween music");
            return false;
        }

        try {
            manager = AudioManager.INSTANCE;
            Class<?> managerType = AudioManager.class;
            activeSounds = accessibleField(managerType, "activeSounds");

            Class<?> soundType = Class.forName("SCP.audioyt_1_20_1.audio.AudioManager$SoundInstance");
            running = accessibleField(soundType, "running");
            payload = accessibleField(soundType, "payload");
            volumeControl = accessibleField(soundType, "volumeControl");
            setVolumeLinear = soundType.getDeclaredMethod("setVolumeLinear", float.class);
            setVolumeLinear.setAccessible(true);

            payloadVolume = accessibleField(PlaySoundPayload.class, "volume");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("JEM Holidays supports the inspected AudioYT 1.0.1 playback layout only", exception);
            manager = null;
            return false;
        }
    }

    private static Field accessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private Object ownedSound() {
        if (ownedSoundId == null || !initialize()) {
            return null;
        }
        try {
            return sounds().get(ownedSoundId);
        } catch (IllegalAccessException exception) {
            disable("Could not inspect JEM Holidays AudioYT playback", exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Object> sounds() throws IllegalAccessException {
        return (Map<UUID, Object>) activeSounds.get(manager);
    }

    private void disable(String message, ReflectiveOperationException exception) {
        LOGGER.error(message, exception);
        if (ownedSoundId != null && manager != null) {
            manager.stop(ownedSoundId);
        }
        ownedSoundId = null;
        manager = null;
        initializationAttempted = true;
    }

    private static float clampVolume(float volume) {
        return Math.max(0.0F, Math.min(1.0F, volume));
    }
}
