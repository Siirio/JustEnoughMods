package com.siirio.jemserver.smp.events.calendar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.siirio.jemserver.smp.SmpConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

public final class HolidayCommands {
    static final String MUTED = "jem_calendar_muted";

    private HolidayCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jem").then(Commands.literal("holiday")
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("mute").then(Commands.argument("muted", BoolArgumentType.bool())
                        .executes(context -> mute(context.getSource(), BoolArgumentType.getBool(context, "muted")))))
                .then(Commands.literal("gift").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> gift(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("accept").then(Commands.argument("gift", UuidArgument.uuid())
                        .executes(context -> accept(context.getSource(), UuidArgument.getUuid(context, "gift")))))
                .then(Commands.literal("cancel").then(Commands.argument("gift", UuidArgument.uuid())
                        .executes(context -> cancel(context.getSource(), UuidArgument.getUuid(context, "gift")))))
                .then(Commands.literal("preview").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("event", StringArgumentType.word())
                                .executes(context -> preview(context.getSource(), StringArgumentType.getString(context, "event")))))
                .then(Commands.literal("force").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("event", StringArgumentType.word())
                                .executes(context -> force(context.getSource(), StringArgumentType.getString(context, "event")))))
                .then(Commands.literal("stop").requires(source -> source.hasPermission(2))
                        .executes(context -> stop(context.getSource())))));
    }

    private static int status(CommandSourceStack source) {
        CalendarEventData data = CalendarEventData.get(source.getServer());
        StringBuilder value = new StringBuilder();
        for (CalendarEvent event : CalendarEvent.values()) {
            if (!value.isEmpty()) value.append(", ");
            value.append(event.name()).append('=').append(data.state(event).map(Enum::name).orElse("UNINITIALIZED"));
        }
        source.sendSuccess(() -> Component.literal(value.toString()), false);
        data.forcedEvent().or(() -> java.util.Arrays.stream(CalendarEvent.values())
                .filter(event -> data.state(event).orElse(null) == CalendarEventState.ACTIVE).findFirst())
                .ifPresent(event -> {
                    ZoneId zone = ZoneId.of(SmpConfig.CALENDAR_TIMEZONE.get());
                    var window = event.window(event.seasonYear(Instant.now(), zone), zone);
                    long seconds = Math.max(0L, java.time.Duration.between(Instant.now(), window.end()).getSeconds());
                    source.sendSuccess(() -> Component.translatable("jem.holiday.active_status", event.translationKey(), formatDuration(seconds)), false);
                });
        return 1;
    }

    private static int mute(CommandSourceStack source, boolean muted) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException().getPersistentData().putBoolean(MUTED, muted);
        source.sendSuccess(() -> Component.translatable(muted ? "jem.holiday.notices_muted" : "jem.holiday.notices_enabled"), false);
        return 1;
    }

    private static int preview(CommandSourceStack source, String raw) {
        CalendarEvent event = parse(raw);
        if (event == null) return 0;
        ZoneId zone = ZoneId.of(SmpConfig.CALENDAR_TIMEZONE.get());
        int year = event.seasonYear(Instant.now(), zone);
        var window = event.window(year, zone);
        source.sendSuccess(() -> Component.literal(event.name() + " " + window.start().atZone(zone) + " — " + window.end().atZone(zone)), false);
        return 1;
    }

    private static int gift(CommandSourceStack source, net.minecraft.server.level.ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        UUID id = HolidayActivities.offerGift(source.getPlayerOrException(), target);
        source.sendSuccess(() -> id == null ? Component.translatable("jem.holiday.gift_unavailable")
                : Component.translatable("jem.holiday.gift_escrowed", id), false);
        return id == null ? 0 : 1;
    }

    private static int accept(CommandSourceStack source, UUID id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        boolean accepted = HolidayActivities.acceptGift(source.getPlayerOrException(), id);
        source.sendSuccess(() -> Component.translatable(accepted ? "jem.holiday.gift_accept_ok" : "jem.holiday.gift_unavailable"), false);
        return accepted ? 1 : 0;
    }

    private static int cancel(CommandSourceStack source, UUID id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        boolean canceled = HolidayActivities.cancelGift(source.getPlayerOrException(), id);
        source.sendSuccess(() -> Component.translatable(canceled ? "jem.holiday.gift_cancel_ok" : "jem.holiday.gift_unavailable"), false);
        return canceled ? 1 : 0;
    }

    private static int force(CommandSourceStack source, String raw) {
        CalendarEvent event = parse(raw);
        if (event == null) return 0;
        CalendarEventData data = CalendarEventData.get(source.getServer());
        data.force(event);
        ZoneId zone = ZoneId.of(SmpConfig.CALENDAR_TIMEZONE.get());
        int year = event.seasonYear(Instant.now(), zone);
        var window = event.window(year, zone);
        data.transition(event, year, CalendarEventState.ACTIVE, window.start(), window.end());
        CalendarEventScheduler.announce(source.getServer(), event, CalendarEventState.ACTIVE);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        CalendarEventData.get(source.getServer()).clearForce();
        return 1;
    }

    private static CalendarEvent parse(String raw) {
        try { return CalendarEvent.valueOf(raw.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { return null; }
    }

    static boolean muted(net.minecraft.server.level.ServerPlayer player) {
        return player.getPersistentData().getBoolean(MUTED);
    }

    private static String formatDuration(long seconds) {
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        return days > 0 ? days + "d " + hours + "h" : hours + "h " + ((seconds % 3_600L) / 60L) + "m";
    }
}
