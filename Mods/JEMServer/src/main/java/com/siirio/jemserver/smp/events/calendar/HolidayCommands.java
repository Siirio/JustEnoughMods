package com.siirio.jemserver.smp.events.calendar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.siirio.jemserver.smp.SmpConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class HolidayCommands {
    private static final String MUTED = "jem_calendar_muted";

    private HolidayCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jem").then(Commands.literal("holiday")
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("mute").then(Commands.argument("muted", BoolArgumentType.bool())
                        .executes(context -> mute(context.getSource(), BoolArgumentType.getBool(context, "muted")))))
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
        return 1;
    }

    private static int mute(CommandSourceStack source, boolean muted) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException().getPersistentData().putBoolean(MUTED, muted);
        source.sendSuccess(() -> Component.literal("Holiday notices muted: " + muted), false);
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

    private static int force(CommandSourceStack source, String raw) {
        CalendarEvent event = parse(raw);
        if (event == null) return 0;
        CalendarEventData data = CalendarEventData.get(source.getServer());
        data.force(event);
        ZoneId zone = ZoneId.of(SmpConfig.CALENDAR_TIMEZONE.get());
        int year = event.seasonYear(Instant.now(), zone);
        var window = event.window(year, zone);
        data.transition(event, year, CalendarEventState.ACTIVE, window.start(), window.end());
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        CalendarEventData.get(source.getServer()).clearForce();
        return 1;
    }

    private static CalendarEvent parse(String raw) {
        try { return CalendarEvent.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { return null; }
    }
}
