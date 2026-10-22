package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.SmpData;
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
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (event.getCommandSelection() != Commands.CommandSelection.DEDICATED) return;
        var type = Commands.argument("event", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(WeeklySchedule.TYPES, builder))
                .executes(context -> startRequested(context.getSource(), StringArgumentType.getString(context, "event"), null))
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                        .executes(context -> startRequested(context.getSource(), StringArgumentType.getString(context, "event"), BlockPosArgument.getBlockPos(context, "position"))));
        var eventRoot = Commands.literal("event").requires(source -> source.hasPermission(2) && EventRuntime.enabled(source.getServer()))
                .then(Commands.literal("start").then(type))
                .then(Commands.literal("stop").then(Commands.argument("id", UuidArgument.uuid())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                SmpData.get(context.getSource().getServer()).events().snapshots().stream()
                                        .filter(row -> !SmpData.closed(row) && row.hasId())
                                        .map(row -> row.id().toString()), builder))
                        .executes(context -> stopExact(context.getSource(), UuidArgument.getUuid(context, "id")))));
        event.getDispatcher().register(Commands.literal("jem").then(eventRoot));
    }

    private static int startRequested(net.minecraft.commands.CommandSourceStack source, String rawType, BlockPos position) {
        String type = rawType.toUpperCase(java.util.Locale.ROOT);
        if (!WeeklySchedule.TYPES.contains(type)) { source.sendFailure(Component.translatable("jem.smp.error.unknown_event")); return 0; }
        if (type.equals("BOSS_RAID")) return startNearestRaid(source, position == null ? BlockPos.containing(source.getPosition()) : position);
        if (position != null && !type.equals("RESOURCE_RUSH") && !type.equals("BLOOD_MOON")) {
            source.sendFailure(Component.translatable("jem.smp.error.position_not_supported")); return 0;
        }
        return start(source, type, position, null, null);
    }

    private static int stopExact(net.minecraft.commands.CommandSourceStack source, UUID id) {
        var serverForMutation = source.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return stopExactInScope(source, id); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> stopExactInScope(source, id));
    }

    private static int stopExactInScope(net.minecraft.commands.CommandSourceStack source, UUID id) {
        var row = SmpData.get(source.getServer()).events().current(id);
        if (row == null || SmpData.closed(row)) { source.sendFailure(Component.translatable("jem.smp.error.event_not_running")); return 0; }
        EventScheduler.finish(source.getServer(), row, "CANCELLED");
        source.sendSuccess(() -> Component.translatable("jem.smp.event_cancelled"), true);
        return 1;
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
            source.sendFailure(Component.translatable("jem.smp.error.raid_target_missing", radius));
            return 0;
        }
        return start(source,"BOSS_RAID",null,null,null,null,raid);
    }

    private static boolean targeted(net.minecraft.server.MinecraftServer server,UUID boss) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return targetedInScope(server, boss); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> targetedInScope(server, boss));
    }

    private static boolean targetedInScope(net.minecraft.server.MinecraftServer server,UUID boss) {
        var data=SmpData.get(server);
        return data.events().currentRows().stream().anyMatch(row->!SmpData.closed(row)
                &&(row.hasTargetBoss()&&row.targetBoss().equals(boss)
                ||row.hasBossEntity()&&row.bossEntity().equals(boss)))
                ||data.parties().currentRows().stream().anyMatch(row->!SmpData.closed(row)&&row.hasBossEntity()&&row.bossEntity().equals(boss));
    }

    private static int start(net.minecraft.commands.CommandSourceStack source,String type,BlockPos position,String waveThree,String waveFive,
                             net.minecraft.world.entity.LivingEntity targetBoss,StructureStaging.RaidTarget raid) {
        var serverForMutation = source.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return startInScope(source, type, position, waveThree, waveFive, targetBoss, raid); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> startInScope(source, type, position, waveThree, waveFive, targetBoss, raid));
    }

    private static int startInScope(net.minecraft.commands.CommandSourceStack source,String type,BlockPos position,String waveThree,String waveFive,
                             net.minecraft.world.entity.LivingEntity targetBoss,StructureStaging.RaidTarget raid) {
        var data=SmpData.get(source.getServer());
        var row=data.events().create(new UUID(0,0));
        if(type.equals("BLOOD_MOON")&&waveThree!=null&&!BloodMoonSetPieces.select(row,waveThree,waveFive)) {
            row.state("CANCELLED");

            source.sendFailure(Component.translatable("jem.smp.error.blood_moon_variants"));
            return 0;
        }
        long now=System.currentTimeMillis();
        row.slot("manual:"+row.id());
        row.activity(type);
        row.title(type);
        row.state("PLANNED");
        row.planned(now);
        if(position!=null) row.requestedPosition(position.asLong());
        if(targetBoss!=null) {
            var arena=com.siirio.jemworldbosstiers.api.RaidArenaApi.arena(targetBoss).orElse(null);
            if(arena==null) {
                EventScheduler.finish(source.getServer(),row,"CANCELLED");
                source.sendFailure(Component.translatable("jem.smp.error.raid_boss_left_arena"));
                return 0;
            }
            var bounds=arena.bounds();
            StructureStaging.attachArena(row,UUID.fromString(arena.id()),arena.entityType(),arena.dimension().toString(),arena.center(),bounds);
            row.targetBoss(targetBoss.getUUID());
            row.bossType(arena.entityType().toString());
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
            source.sendFailure(Component.translatable("jem.smp.error.raid_boss_unavailable"));
            return 0;
        }
        data.events().prune();
        source.sendSuccess(()->Component.literal(row.id().toString()),false);
        return 1;
    }

    private EventCommands() {}
}
