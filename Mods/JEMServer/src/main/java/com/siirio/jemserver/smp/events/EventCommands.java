package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.BossHostingPrompt;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class EventCommands {
    private static final String ADMINISTRATOR_NAME="Sirio_o";
    private static final java.util.Map<UUID, String> TEST_PLAYERS = java.util.Map.of(
            UUID.nameUUIDFromBytes("jem:test:ember".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "Ember_Test",
            UUID.nameUUIDFromBytes("jem:test:frost".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "Frost_Test");

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (event.getCommandSelection() != Commands.CommandSelection.DEDICATED) return;
        var root = Commands.literal("smp").then(Commands.literal("event")
            .requires(EventCommands::administrator)
            .then(Commands.literal("stop").then(Commands.literal("combat").executes(context -> stopCombat(context.getSource().getServer()))))
            .then(Commands.literal("stop").then(Commands.literal("all").executes(context -> stopAll(context.getSource().getServer()))))
            .then(Commands.literal("simulate").executes(context -> simulate(context.getSource().getServer())))
            .then(Commands.literal("schedule")
                .then(Commands.literal("list").executes(context -> listSchedule(context.getSource())))
                .then(Commands.literal("reset").executes(context -> resetSchedule(context.getSource())))
                .then(Commands.literal("add")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(WeeklySchedule.TYPES, builder))
                        .then(Commands.argument("day", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(java.time.DayOfWeek.values()).map(Enum::name), builder))
                            .then(Commands.argument("time", StringArgumentType.word())
                                .executes(context -> changeSchedule(context.getSource(), true,
                                        StringArgumentType.getString(context, "type"),
                                        StringArgumentType.getString(context, "day"),
                                        StringArgumentType.getString(context, "time")))))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(WeeklySchedule.TYPES, builder))
                        .then(Commands.argument("day", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(java.time.DayOfWeek.values()).map(Enum::name), builder))
                            .then(Commands.argument("time", StringArgumentType.word())
                                .executes(context -> changeSchedule(context.getSource(), false,
                                        StringArgumentType.getString(context, "type"),
                                        StringArgumentType.getString(context, "day"),
                                        StringArgumentType.getString(context, "time"))))))))
            .then(Commands.literal("stop").then(Commands.argument("id", UuidArgument.uuid()).executes(context -> {
                var server = context.getSource().getServer();
                var row = SmpData.get(server).find("events", UuidArgument.getUuid(context, "id"));
                if (row == null || SmpData.closed(row)) return 0;
                EventScheduler.finish(server, row, "CANCELLED");
                context.getSource().sendSuccess(() -> Component.translatable("jem.smp.event_cancelled"), true);
                return 1;
            }))));
        event.getDispatcher().register(root);
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("arena")
                .then(Commands.argument("id", UuidArgument.uuid()).executes(context -> {
                    try {
                        StructureStaging.prompt(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "id"));
                    } catch(IllegalArgumentException failure) {
                        context.getSource().sendFailure(Component.translatable("jem.smp.error."+failure.getMessage()));
                    }
                    return 1;
                }))));
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("boss")
                .executes(context -> reviveBoss(context.getSource()))
                .then(Commands.literal("revive").requires(EventCommands::administrator).executes(context -> reviveBoss(context.getSource())))
                .then(Commands.argument("id", UuidArgument.uuid()).executes(context -> {
                    try {
                        BossHostingPrompt.prompt(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "id"));
                    } catch(IllegalArgumentException failure) {
                        context.getSource().sendFailure(Component.translatable("jem.smp.error."+failure.getMessage()));
                    }
                    return 1;
                }))));
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("event-open")
                .then(Commands.argument("id", UuidArgument.uuid()).executes(context -> {
                    EventAreaHooks.prompt(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "id"));
                    return 1;
                }))));
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("event-return")
                .then(Commands.argument("event",UuidArgument.uuid())
                        .then(Commands.argument("session",UuidArgument.uuid())
                                .then(Commands.argument("choice",StringArgumentType.word()).executes(context->eventReturn(
                                        context.getSource().getPlayerOrException(),UuidArgument.getUuid(context,"event"),
                                        UuidArgument.getUuid(context,"session"),StringArgumentType.getString(context,"choice"))))))));
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("vote")
                .then(Commands.argument("event",UuidArgument.uuid())
                        .then(Commands.literal("continue").executes(context->vote(context.getSource(),UuidArgument.getUuid(context,"event"),true)))
                        .then(Commands.literal("cashout").executes(context->vote(context.getSource(),UuidArgument.getUuid(context,"event"),false))))));
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("forfeit")
                .then(Commands.argument("party",UuidArgument.uuid()).executes(context->{
                    try {
                        com.siirio.jemserver.smp.HostedParties.forfeit(context.getSource().getPlayerOrException(),UuidArgument.getUuid(context,"party"));
                    } catch(IllegalArgumentException failure) {
                        context.getSource().sendFailure(Component.translatable("jem.smp.error."+failure.getMessage()));
                    }
                    return 1;
                }))));
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("encounter-leave")
                .then(Commands.argument("party",UuidArgument.uuid()).executes(context->{
                    try {
                        com.siirio.jemserver.smp.HostedParties.leaveEncounter(context.getSource().getPlayerOrException(),UuidArgument.getUuid(context,"party"));
                    } catch(IllegalArgumentException failure) {
                        context.getSource().sendFailure(Component.translatable("jem.smp.error."+failure.getMessage()));
                    }
                    return 1;
                }))));
        for (String type : java.util.List.of("FISHING", "RESOURCE_RUSH", "BLOOD_MOON", "BOSS_RAID", "COOKING_SHOW")) registerStart(event,type);
    }

    private static void registerStart(RegisterCommandsEvent event,String type) {
        var command=Commands.literal(type.toLowerCase(java.util.Locale.ROOT));
        if(type.equals("BOSS_RAID")) command.executes(context->startNearestRaid(context.getSource(),BlockPos.containing(context.getSource().getPosition())));
        else command.executes(context->start(context.getSource(),type,null,null,null));
        if(type.equals("RESOURCE_RUSH")||type.equals("BLOOD_MOON")) {
            var position=Commands.argument("position",BlockPosArgument.blockPos())
                    .executes(context->start(context.getSource(),type,BlockPosArgument.getBlockPos(context,"position"),null,null));
            if(type.equals("BLOOD_MOON")) position.then(waveSelections(true));
            command.then(position);
        }
        if(type.equals("BOSS_RAID")) command.then(Commands.argument("position",BlockPosArgument.blockPos())
                .executes(context->startNearestRaid(context.getSource(),BlockPosArgument.getBlockPos(context,"position"))));
        if(type.equals("BLOOD_MOON")) command.then(waveSelections(false));
        event.getDispatcher().register(Commands.literal("smp").then(Commands.literal("event")
                .requires(EventCommands::administrator).then(Commands.literal("start").then(command))));
    }

    private static boolean administrator(net.minecraft.commands.CommandSourceStack source) {
        return source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && player.getGameProfile().getName().equalsIgnoreCase(ADMINISTRATOR_NAME);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<net.minecraft.commands.CommandSourceStack,String> waveSelections(boolean positioned) {
        return Commands.argument("wave3",StringArgumentType.word())
                .suggests((context,builder)->SharedSuggestionProvider.suggest(BloodMoonSetPieces.waveThreeSelections(),builder))
                .then(Commands.argument("wave5",StringArgumentType.word())
                        .suggests((context,builder)->SharedSuggestionProvider.suggest(BloodMoonSetPieces.waveFiveSelections(),builder))
                        .executes(context->start(context.getSource(),"BLOOD_MOON",positioned?BlockPosArgument.getBlockPos(context,"position"):null,
                                StringArgumentType.getString(context,"wave3"),StringArgumentType.getString(context,"wave5"))));
    }

    private static int start(net.minecraft.commands.CommandSourceStack source,String type,BlockPos position,String waveThree,String waveFive) {
        return start(source,type,position,waveThree,waveFive,null,null);
    }

    private static int startNearestRaid(net.minecraft.commands.CommandSourceStack source,BlockPos reference) {
        var level=source.getLevel();
        int radius=EventRules.RAID_SEARCH_RADIUS.get();
        var center=net.minecraft.world.phys.Vec3.atCenterOf(reference);
        var boss=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(reference).inflate(radius),
                        entity->com.siirio.jemworldbosstiers.api.HostedEncounterApi.canPrepareRaid(entity)&&!targeted(source.getServer(),entity.getUUID()))
                .stream().min(java.util.Comparator.comparingDouble((net.minecraft.world.entity.LivingEntity entity)->entity.distanceToSqr(center))
                        .thenComparing(entity->entity.getUUID().toString())).orElse(null);
        if(boss!=null) return start(source,"BOSS_RAID",null,null,null,boss,null);
        var raid=StructureStaging.availableRaid(level,reference);
        if(raid==null) {
            source.sendFailure(Component.literal("No eligible defeated raid boss or unlocked raid arena within "+radius+" blocks"));
            return 0;
        }
        return start(source,"BOSS_RAID",null,null,null,null,raid);
    }

    private static int reviveBoss(net.minecraft.commands.CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!administrator(source)) {
            source.sendFailure(Component.translatable("jem.smp.error.administrator_required"));
            return 0;
        }
        var result = com.siirio.jemworldbosstiers.api.BossRevivalApi.reviveNearest(source.getPlayerOrException(), EventRules.RAID_SEARCH_RADIUS.get());
        if (result == com.siirio.jemworldbosstiers.api.BossRevivalApi.Result.CREATED) {
            source.sendSuccess(() -> Component.translatable("jem.smp.boss_revive.created"), true);
            return 1;
        }
        source.sendFailure(Component.translatable("jem.smp.boss_revive." + result.name().toLowerCase(java.util.Locale.ROOT)));
        return 0;
    }

    private static boolean targeted(net.minecraft.server.MinecraftServer server,UUID boss) {
        var data=SmpData.get(server);
        return data.all("events").stream().anyMatch(row->!SmpData.closed(row)
                &&(row.hasUUID("targetBoss")&&row.getUUID("targetBoss").equals(boss)
                ||row.hasUUID("bossEntity")&&row.getUUID("bossEntity").equals(boss)))
                ||data.all("parties").stream().anyMatch(row->!SmpData.closed(row)&&row.hasUUID("bossEntity")&&row.getUUID("bossEntity").equals(boss));
    }

    private static int start(net.minecraft.commands.CommandSourceStack source,String type,BlockPos position,String waveThree,String waveFive,
                             net.minecraft.world.entity.LivingEntity targetBoss,StructureStaging.RaidTarget raid) {
        var data=SmpData.get(source.getServer());
        var row=data.create("events",new UUID(0,0));
        if(type.equals("BLOOD_MOON")&&waveThree!=null&&!BloodMoonSetPieces.select(row,waveThree,waveFive)) {
            row.putString("state","CANCELLED");
            data.changed(row);
            source.sendFailure(Component.literal("Wave 3 and Wave 5 variants must come from their matching Blood Moon lists"));
            return 0;
        }
        long now=System.currentTimeMillis();
        row.putString("slot","manual:"+row.getUUID("id"));
        row.putString("activity",type);
        row.putString("title",type);
        row.putString("state","PLANNED");
        row.putLong("planned",now);
        if(position!=null) row.putLong("requestedPosition",position.asLong());
        if(targetBoss!=null) {
            var arena=com.siirio.jemworldbosstiers.api.RaidArenaApi.arena(targetBoss).orElse(null);
            if(arena==null) {
                EventScheduler.finish(source.getServer(),row,"CANCELLED");
                source.sendFailure(Component.literal("Nearest raid boss is no longer inside its registered structure arena"));
                return 0;
            }
            var bounds=arena.bounds();
            StructureStaging.attachArena(row,UUID.fromString(arena.id()),arena.entityType(),arena.dimension().toString(),arena.center(),bounds);
            row.putUUID("targetBoss",targetBoss.getUUID());
            row.putString("bossType",arena.entityType().toString());
        }
        if(raid!=null&&!RaidEvent.claimArena(source.getServer(),row,raid.id().toString(),raid.boss(),raid.dimension(),raid.center(),raid.bounds())) {
            EventScheduler.finish(source.getServer(),row,"CANCELLED");
            source.sendFailure(Component.translatable("jem.smp.error.raid_unavailable"));
            return 0;
        }
        if(!EventScheduler.start(source.getServer(),row,now)) {
            EventScheduler.finish(source.getServer(),row,"CANCELLED");
            source.sendFailure(Component.translatable("jem.smp.error.event_cannot_start"));
            return 0;
        }
        if(targetBoss!=null&&!com.siirio.jemworldbosstiers.api.HostedEncounterApi.hold(targetBoss)) {
            EventScheduler.cancelPreparation(source.getServer(),row);
            source.sendFailure(Component.literal("Nearest raid boss is no longer available"));
            return 0;
        }
        data.prune("events");
        source.sendSuccess(()->Component.literal(row.getUUID("id").toString()),false);
        return 1;
    }

    private static int vote(net.minecraft.commands.CommandSourceStack source,UUID event,boolean continueBattle) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BloodMoonVoting.vote(source.getPlayerOrException(),event,continueBattle);
        return 1;
    }

    private static int eventReturn(net.minecraft.server.level.ServerPlayer player,UUID eventId,UUID session,String choice) {
        var row=SmpData.get(player.server).find("events",eventId);
        var party=row!=null&&row.hasUUID("party")?SmpData.get(player.server).find("parties",row.getUUID("party"))
                :SmpData.get(player.server).find("parties",eventId);
        if(party==null) return 0;
        if(choice.equals("stay")) return PartyTeleportFlow.choose(player,party,session,false)?1:0;
        if(choice.equals("return")) return PartyTeleportFlow.choose(player,party,session,true)?1:0;
        return 0;
    }

    private static int listSchedule(net.minecraft.commands.CommandSourceStack source) {
        var definitions = EventSchedule.definitions(source.getServer());
        source.sendSuccess(() -> Component.literal("Event schedule (Astana, Asia/Almaty):"), false);
        definitions.forEach(definition -> source.sendSuccess(() -> Component.literal(definition), false));
        return definitions.size();
    }

    private static int resetSchedule(net.minecraft.commands.CommandSourceStack source) {
        EventSchedule.reset(source.getServer());
        source.sendSuccess(() -> Component.literal("Event schedule reset to server config defaults"), true);
        return 1;
    }

    private static int changeSchedule(
            net.minecraft.commands.CommandSourceStack source,
            boolean add,
            String type,
            String day,
            String time) {
        try {
            String definition = definition(type, day, time);
            var definitions = new java.util.ArrayList<>(EventSchedule.definitions(source.getServer()));
            boolean changed = add ? !definitions.contains(definition) && definitions.add(definition) : definitions.remove(definition);
            if (!changed) {
                source.sendFailure(Component.literal(add ? "Schedule entry already exists" : "Schedule entry not found"));
                return 0;
            }
            EventSchedule.replace(source.getServer(), definitions);
            source.sendSuccess(() -> Component.literal((add ? "Added " : "Removed ") + definition), true);
            return 1;
        } catch (RuntimeException invalid) {
            source.sendFailure(Component.literal("Invalid schedule entry. Use EVENT DAY HH:mm in Astana time"));
            return 0;
        }
    }

    private static String definition(String type, String day, String time) {
        String normalizedType = type.toUpperCase(java.util.Locale.ROOT);
        String normalizedDay = day.toUpperCase(java.util.Locale.ROOT);
        if (!WeeklySchedule.TYPES.contains(normalizedType)) throw new IllegalArgumentException();
        java.time.DayOfWeek.valueOf(normalizedDay);
        String[] clock = time.split(":", -1);
        if (clock.length != 2) throw new IllegalArgumentException();
        var parsed = java.time.LocalTime.of(Integer.parseInt(clock[0]), Integer.parseInt(clock[1]));
        return normalizedType + ":" + normalizedDay + ":" + String.format(java.util.Locale.ROOT, "%02d:%02d", parsed.getHour(), parsed.getMinute());
    }

    private static int stopCombat(net.minecraft.server.MinecraftServer server) {
        return stop(server, false);
    }

    private static int stopAll(net.minecraft.server.MinecraftServer server) {
        return stop(server, true);
    }

    private static int stop(net.minecraft.server.MinecraftServer server, boolean allEvents) {
        var data = SmpData.get(server);
        int stopped = 0;
        for (var party : data.all("parties")) {
            if (!party.getString("state").equals("ACTIVE") || !party.getString("activity").equals("BOSS")) continue;
            var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(party.getString("dimension"))));
            var boss = level == null || !party.hasUUID("bossEntity") ? null : level.getEntity(party.getUUID("bossEntity"));
            if (boss instanceof net.minecraft.world.entity.LivingEntity living && com.siirio.jemworldbosstiers.api.HostedEncounterApi.cancel(living)) {
                stopped++;
            } else {
                party.putString("state", "CANCELLED");
                data.changed(party);
                stopped++;
            }
        }
        for (var row : data.all("events")) {
            if (!row.getString("state").equals("ACTIVE") && !row.getBoolean("preparing")
                    || !allEvents && !java.util.Set.of("BLOOD_MOON", "BOSS_RAID").contains(row.getString("activity"))) continue;
            if (row.getBoolean("preparing")) EventScheduler.cancelPreparation(server, row);
            else EventScheduler.finish(server, row, "CANCELLED");
            stopped++;
        }
        int result = stopped;
        server.sendSystemMessage(Component.literal("Stopped active sessions: " + result));
        return result;
    }

    private static int simulate(net.minecraft.server.MinecraftServer server) {
        var data = SmpData.get(server);
        int changed = 0;
        for (var event : data.all("events")) {
            if (!event.getString("state").equals("ACTIVE")) continue;
            addTestPlayers(event);
            data.changed(event);
            if (event.hasUUID("party")) {
                var party = data.find("parties", event.getUUID("party"));
                if (party != null) {
                    addTestPlayers(party);
                    data.changed(party);
                }
            }
            changed++;
        }
        for (var party : data.all("parties")) {
            if (!party.getString("state").equals("ACTIVE")) continue;
            addTestPlayers(party);
            data.changed(party);
            changed++;
        }
        int result = changed;
        server.sendSystemMessage(Component.literal("Test participants attached to active sessions: " + result));
        return result;
    }

    private static void addTestPlayers(net.minecraft.nbt.CompoundTag row) {
        int index = 0;
        for (var test : TEST_PLAYERS.entrySet()) {
            var member = com.siirio.jemserver.smp.SmpRecords.members(row).getCompound(test.getKey().toString());
            member.putString("name", test.getValue());
            member.putBoolean("accepted", true);
            member.putBoolean("ready", true);
            member.putBoolean("simulated", true);
            member.putFloat("hp", index == 0 ? 18 : 14);
            member.putFloat("maxHp", 20);
            member.putInt("ping", index == 0 ? 46 : 91);
            com.siirio.jemserver.smp.SmpRecords.members(row).put(test.getKey().toString(), member);
            index++;
        }
    }
    private EventCommands() {}
}
