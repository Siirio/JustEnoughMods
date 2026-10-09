package com.siirio.jemserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;

public final class SleepVote {
    private static final long DAY_TICKS = 24_000;
    private final Set<UUID> attemptingSleep = new HashSet<>();
    private final Map<ResourceKey<Level>, Long> completedNights = new HashMap<>();
    private SleepBallot ballot = new SleepBallot();
    private Set<UUID> eligible = Set.of();
    private Set<UUID> sleeping = Set.of();
    private ResourceKey<Level> dimension;
    private UUID round;
    private long deadline;
    private boolean completing;

    public SleepVote() {}

    public void attempt(ServerPlayer player) {
        if (ServerConfig.SLEEP_ENABLED.get()) attemptingSleep.add(player.getUUID());
    }

    public void tick(MinecraftServer server) {
        if (!ServerConfig.SLEEP_ENABLED.get()) { close(server); return; }
        if (dimension != null) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null || level.isDay()) close(server);
        }
        var attempts = Set.copyOf(attemptingSleep);
        attemptingSleep.clear();
        for (UUID id : attempts) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (dimension == null && player != null && !player.isSpectator() && player.isSleeping()
                    && player.serverLevel().isNight() && player.serverLevel().canSleepThroughNights()) {
                long night = Math.floorDiv(player.serverLevel().getDayTime(), DAY_TICKS);
                if (completedNights.getOrDefault(player.level().dimension(), Long.MIN_VALUE) != night) start(player);
            }
        }
        if (dimension == null) return;
        refresh(server);
        Set<UUID> nextSleeping=server.getPlayerList().getPlayers().stream()
                .filter(player->eligible.contains(player.getUUID())&&player.level().dimension().equals(dimension)&&player.isSleeping())
                .map(ServerPlayer::getUUID).collect(Collectors.toUnmodifiableSet());
        Set<UUID> newlySleeping=new HashSet<>(nextSleeping);
        newlySleeping.removeAll(sleeping);
        sleeping=nextSleeping;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (sleeping.contains(player.getUUID())) ballot.cast(player.getUUID(), true, eligible);
            if (newlySleeping.contains(player.getUUID())) broadcastChoice(server,player,"jem.sleep.sleeping",ChatFormatting.AQUA);
        }
        if (System.nanoTime() >= deadline) finish(server);
    }

    private void start(ServerPlayer player) {
        dimension = player.level().dimension();
        round = UUID.randomUUID();
        ballot = new SleepBallot();
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(ServerConfig.SLEEP_VOTE_SECONDS.get());
        refresh(player.server);
        sleeping=Set.of(player.getUUID());
        ballot.cast(player.getUUID(), true, eligible);
        Component agree = Component.translatable("jem.sleep.agree").withStyle(style -> style.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sleep agree")));
        Component disagree = Component.translatable("jem.sleep.disagree").withStyle(style -> style.withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sleep disagree")));
        for(ServerPlayer recipient:player.server.getPlayerList().getPlayers()) {
            var message=Component.translatable("jem.sleep.started",player.getGameProfile().getName(),ServerConfig.SLEEP_VOTE_SECONDS.get());
            if(!recipient.isSleeping()) message.append(" ").append(agree).append(" ").append(disagree);
            recipient.sendSystemMessage(message);
        }
        broadcastChoice(player.server,player,"jem.sleep.sleeping",ChatFormatting.AQUA);
    }

    public int vote(ServerPlayer player, boolean agree) { return vote(player, round, agree); }

    public int vote(ServerPlayer player, UUID requestedRound, boolean agree) {
        if (!ServerConfig.SLEEP_ENABLED.get() || dimension == null || !round.equals(requestedRound) || System.nanoTime() >= deadline) return 0;
        if (player.isSleeping() && player.level().dimension().equals(dimension)) {
            return 0;
        }
        refresh(player.server);
        if (!ballot.cast(player.getUUID(), agree, eligible)) {
            return 0;
        }
        broadcastChoice(player.server,player,agree?"jem.sleep.agree":"jem.sleep.disagree",agree?ChatFormatting.GREEN:ChatFormatting.RED);
        return 1;
    }

    public void rosterChanged(MinecraftServer server) {
        if (dimension != null) refresh(server);
    }

    private boolean refresh(MinecraftServer server) {
        var next = server.getPlayerList().getPlayers().stream().filter(player -> !player.isSpectator())
                .map(ServerPlayer::getUUID).collect(Collectors.toUnmodifiableSet());
        boolean changed = !next.equals(eligible);
        eligible = next;
        return changed;
    }

    public void finishing(SleepFinishedTimeEvent event) {
        if (!completing && ServerConfig.SLEEP_ENABLED.get() && event.getLevel() instanceof ServerLevel level && level.isNight())
            event.setTimeAddition(level.getDayTime());
    }

    private void finish(MinecraftServer server) {
        ServerLevel level = server.getLevel(dimension);
        boolean approved = ballot.approved(eligible);
        if (level != null) {
            completedNights.put(dimension, Math.floorDiv(level.getDayTime(), DAY_TICKS));
            if (approved && level.isNight() && level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                long current = level.getDayTime();
                long next = current + DAY_TICKS;
                completing = true;
                try { level.setDayTime(ForgeEventFactory.onSleepFinished(level, next - next % DAY_TICKS, current)); }
                finally { completing = false; }
                if (level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) level.setWeatherParameters(0, 0, false, false);
            } else approved = false;
            level.players().stream().filter(ServerPlayer::isSleeping).toList().forEach(player -> player.stopSleepInBed(false, false));
        } else approved = false;
        broadcast(server, Component.translatable(approved ? "jem.sleep.passed" : "jem.sleep.failed",
                ballot.agreements(eligible), ballot.disagreements(eligible)));
        close(server);
    }

    private void close(MinecraftServer server) {
        dimension = null;
        round = null;
        eligible = Set.of();
        sleeping = Set.of();
        ballot = new SleepBallot();
        attemptingSleep.clear();
    }

    public void reset() {
        dimension = null;
        round = null;
        eligible = Set.of();
        sleeping = Set.of();
        ballot = new SleepBallot();
        attemptingSleep.clear();
        completedNights.clear();
    }

    private static void broadcast(MinecraftServer server, Component message) { server.getPlayerList().broadcastSystemMessage(message, false); }

    private static void broadcastChoice(MinecraftServer server,ServerPlayer player,String translation,ChatFormatting color) {
        broadcast(server,Component.literal(player.getGameProfile().getName()+" — ")
                .append(Component.translatable(translation).withStyle(color)));
    }
}
