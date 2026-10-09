package com.siirio.jemvillagertalking.client.speech;

import com.siirio.jemvillagertalking.speech.VillagerSpeechCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientVillagerSpeech {
    private static final int SCREEN_MARGIN = 12;
    private static final int HOTBAR_CLEARANCE = 78;
    private static final int CHAT_CLEARANCE = 12;
    private static final int TEXT_PADDING = 4;
    private static final int LINE_SPACING = 2;
    private static final int MAX_TEXT_WIDTH = 420;
    private static final int BACKGROUND_COLOR = 0x98000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final List<ActiveSpeech> ACTIVE = new ArrayList<>();
    private static long sequence;
    private static ActiveSpeech subtitle;
    private static long subtitleEnds;

    private ClientVillagerSpeech() {
    }

    public static void accept(
            String lineId,
            int delayTicks,
            boolean replaceSubtitle,
            boolean showAuthor,
            String speakerRole,
            String speakerName,
            int authorOrdinal
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || VillagerSpeechCatalog.line(lineId) == null) {
            return;
        }
        ActiveSpeech speech = new ActiveSpeech(
                lineId,
                minecraft.level.getGameTime() + delayTicks,
                sequence++,
                replaceSubtitle,
                showAuthor,
                speakerRole,
                speakerName,
                authorOrdinal
        );
        if (replaceSubtitle && delayTicks == 0) {
            ACTIVE.clear();
            ACTIVE.add(speech);
            speech.started = true;
            subtitle = speech;
            subtitleEnds = minecraft.level.getGameTime() + VillagerSpeechCatalog.DISPLAY_TICKS;
        } else {
            ACTIVE.add(speech);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            subtitle = null;
            return;
        }
        long now = minecraft.level.getGameTime();
        if (subtitle != null && now >= subtitleEnds) {
            subtitle = null;
        }
        ActiveSpeech replacement = ACTIVE.stream()
                .filter(speech -> !speech.started && speech.replaceSubtitle && now >= speech.startTick)
                .max(Comparator.comparingLong(ActiveSpeech::sequence))
                .orElse(null);
        if (replacement != null) {
            ACTIVE.stream()
                    .filter(speech -> !speech.started && speech.replaceSubtitle && now >= speech.startTick)
                    .forEach(speech -> speech.started = true);
            subtitle = replacement;
            subtitleEnds = now + VillagerSpeechCatalog.DISPLAY_TICKS;
        }
        if (subtitle == null) {
            subtitle = ACTIVE.stream()
                    .filter(speech -> !speech.started && now >= speech.startTick)
                    .min(Comparator.comparingLong(ActiveSpeech::startTick).thenComparingLong(ActiveSpeech::sequence))
                    .orElse(null);
            if (subtitle != null) {
                subtitleEnds = now + VillagerSpeechCatalog.DISPLAY_TICKS;
            }
        }
        Iterator<ActiveSpeech> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            ActiveSpeech speech = iterator.next();
            if (!speech.started && now >= speech.startTick) {
                speech.started = true;
            }
            if (now > speech.startTick + VillagerSpeechCatalog.DISPLAY_TICKS) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void renderSubtitle(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || !canRender(minecraft)) {
            return;
        }
        render(event.getGuiGraphics(), minecraft.getWindow().getGuiScaledHeight() - HOTBAR_CLEARANCE);
    }

    @SubscribeEvent
    public static void renderOverChat(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof ChatScreen) || !canRender(minecraft)) {
            return;
        }
        int chatTop = minecraft.getWindow().getGuiScaledHeight() - minecraft.gui.getChat().getHeight();
        render(event.getGuiGraphics(), chatTop - CHAT_CLEARANCE);
    }

    private static boolean canRender(Minecraft minecraft) {
        return subtitle != null && minecraft.level != null && minecraft.player != null
                && !minecraft.options.hideGui && minecraft.level.getGameTime() < subtitleEnds;
    }

    private static void render(GuiGraphics graphics, int bottomY) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int textWidth = Math.min(MAX_TEXT_WIDTH, screenWidth - SCREEN_MARGIN * 2);
        String language = minecraft.getLanguageManager().getSelected();
        Component content = subtitle.content(language);
        List<FormattedCharSequence> lines = minecraft.font.split(content, textWidth);
        int lineHeight = minecraft.font.lineHeight + LINE_SPACING;
        int contentHeight = lines.size() * lineHeight - LINE_SPACING;
        int topY = Math.max(SCREEN_MARGIN, bottomY - contentHeight);
        int widestLine = lines.stream().mapToInt(minecraft.font::width).max().orElse(0);
        int centerX = screenWidth / 2;
        graphics.fill(centerX - widestLine / 2 - TEXT_PADDING, topY - TEXT_PADDING,
                centerX + widestLine / 2 + TEXT_PADDING, topY + contentHeight + TEXT_PADDING, BACKGROUND_COLOR);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawCenteredString(minecraft.font, lines.get(index), centerX, topY + index * lineHeight, TEXT_COLOR);
        }
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
        subtitle = null;
    }

    private static final class ActiveSpeech {
        private final VillagerSpeechCatalog.Line line;
        private final long startTick;
        private final long sequence;
        private final boolean replaceSubtitle;
        private final boolean showAuthor;
        private final String speakerRole;
        private final String speakerName;
        private final int authorOrdinal;
        private boolean started;

        private ActiveSpeech(
                String lineId,
                long startTick,
                long sequence,
                boolean replaceSubtitle,
                boolean showAuthor,
                String speakerRole,
                String speakerName,
                int authorOrdinal
        ) {
            this.line = VillagerSpeechCatalog.line(lineId);
            this.startTick = startTick;
            this.sequence = sequence;
            this.replaceSubtitle = replaceSubtitle;
            this.showAuthor = showAuthor;
            this.speakerRole = speakerRole;
            this.speakerName = speakerName;
            this.authorOrdinal = authorOrdinal;
        }

        private VillagerSpeechCatalog.Line line() {
            return line;
        }

        private long startTick() {
            return startTick;
        }

        private long sequence() {
            return sequence;
        }

        private Component content(String language) {
            MutableComponent text = Component.literal(line.text(language)).withStyle(ChatFormatting.WHITE);
            if (!showAuthor) {
                return text;
            }
            MutableComponent author = speakerName.isBlank()
                    ? Component.translatable(speakerTranslationKey(speakerRole))
                    : Component.literal(speakerName);
            return author.withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" " + authorOrdinal + ": ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(text);
        }

        private static String speakerTranslationKey(String role) {
            return switch (role) {
                case "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
                        "leatherworker", "librarian", "mason", "nitwit", "shepherd", "toolsmith", "unemployed",
                        "weaponsmith", "child" -> "jem_villager_talking.speaker." + role;
                default -> "jem_villager_talking.speaker.villager";
            };
        }
    }
}
