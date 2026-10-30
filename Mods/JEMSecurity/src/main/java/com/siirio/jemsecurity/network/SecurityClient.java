package com.siirio.jemsecurity.network;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.siirio.jemsecurity.client.ResourcePackGuard;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = "jem_security", value = Dist.CLIENT)
public final class SecurityClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_LOG_BYTES = 128 * 1024;
    private static final int CHUNK_CHARS = 16_000;
    private static final Pattern SECRET = Pattern.compile("(?i)(access[_-]?token|authorization|bearer|password|secret)(\\s*[:=]\\s*)\\S+");
    private static final Pattern USER_PATH = Pattern.compile("(?i)[A-Z]:\\\\Users\\\\[^\\\\\\s]+");
    private static final Pattern IPV4 = Pattern.compile("(?<![0-9])(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?");
    private static final Map<UUID, Long> LOG_REQUESTS = new HashMap<>();
    private static volatile SecurityNetwork.Attestation cachedAttestation;

    private SecurityClient() { }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        cachedAttestation = snapshot();
        send(cachedAttestation, "login");
    }

    public static void respondToAttestationRequest() {
        SecurityNetwork.Attestation attestation = cachedAttestation;
        if (attestation == null) {
            attestation = snapshot();
            cachedAttestation = attestation;
        }
        send(attestation, "server_challenge");
    }

    public static void resourcePacksChanged() {
        cachedAttestation = snapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) send(cachedAttestation, "resource_pack_change");
    }

    public static void reportBlockedPackAttempt(String packId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null && packId != null && !packId.isBlank())
            SecurityNetwork.sendPackAttempt(packId);
    }

    private static SecurityNetwork.Attestation snapshot() {
        List<String> mods = ModList.get().getMods().stream().map(info -> info.getModId().toLowerCase(Locale.ROOT)).sorted().toList();
        var selectedPacks = Minecraft.getInstance().getResourcePackRepository().getSelectedPacks();
        List<String> packs = selectedPacks.stream()
                .map(pack -> pack.getId().toLowerCase(Locale.ROOT)).sorted().toList();
        String blockedPack = ResourcePackGuard.findForbiddenPacks(selectedPacks.stream());
        String version = ModList.get().getModContainerById("jem_security")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown");
        return new SecurityNetwork.Attestation(mods, packs, version, blockedPack == null ? "" : blockedPack);
    }

    private static void send(SecurityNetwork.Attestation attestation, String trigger) {
        SecurityNetwork.sendAttestation(attestation.mods(), attestation.packs(), attestation.clientVersion(), attestation.blockedPackId());
        LOGGER.info("JEM Security sent client attestation from {} with {} mods and {} resource packs",
                trigger, attestation.mods().size(), attestation.packs().size());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        LOG_REQUESTS.clear();
        cachedAttestation = null;
    }

    @SubscribeEvent
    public static void clientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("jemsecurity-consent")
                .then(net.minecraft.commands.Commands.argument("token", StringArgumentType.word())
                        .executes(context -> sendLog(StringArgumentType.getString(context, "token")))));
    }

    public static void receiveLogRequest(UUID token) {
        LOG_REQUESTS.put(token, System.currentTimeMillis() + 60_000L);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.player.sendSystemMessage(Component.translatable("jem_security.log_request").withStyle(ChatFormatting.YELLOW));
        minecraft.player.sendSystemMessage(Component.translatable("jem_security.log_send")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jemsecurity-consent " + token))));
    }

    private static int sendLog(String tokenText) {
        UUID token;
        try {
            token = UUID.fromString(tokenText);
        } catch (IllegalArgumentException exception) {
            return 0;
        }
        Long expires = LOG_REQUESTS.remove(token);
        if (expires == null || expires < System.currentTimeMillis()) return 0;
        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("latest.log");
        String sanitized;
        try {
            sanitized = sanitize(readTail(file));
        } catch (IOException exception) {
            return 0;
        }
        List<String> chunks = split(sanitized);
        for (int index = 0; index < chunks.size(); index++) SecurityNetwork.sendLogChunk(token, index, chunks.size(), chunks.get(index));
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("jem_security.log_sent").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static String readTail(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return "latest.log is not present";
        try (RandomAccessFile input = new RandomAccessFile(file.toFile(), "r")) {
            long length = input.length();
            int size = (int) Math.min(length, MAX_LOG_BYTES);
            byte[] bytes = new byte[size];
            input.seek(length - size);
            input.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String sanitize(String text) {
        String value = SECRET.matcher(text).replaceAll("$1$2[REDACTED]");
        value = USER_PATH.matcher(value).replaceAll("%USERPROFILE%");
        return IPV4.matcher(value).replaceAll("[IP]");
    }

    private static List<String> split(String text) {
        if (text.isEmpty()) return List.of("");
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < text.length(); start += CHUNK_CHARS)
            chunks.add(text.substring(start, Math.min(text.length(), start + CHUNK_CHARS)));
        return chunks.size() <= 8 ? chunks : chunks.subList(chunks.size() - 8, chunks.size());
    }
}
