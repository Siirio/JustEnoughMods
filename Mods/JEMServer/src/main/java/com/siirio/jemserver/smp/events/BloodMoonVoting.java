package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.CombatNetwork;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.EventState;
import com.siirio.jemserver.smp.EventVote;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.UUID;

public final class BloodMoonVoting {
    private static final String CONTINUE = "CONTINUE";
    private static final String CASH_OUT = "CASH_OUT";
    private static final long TIMEOUT_MILLIS = 20_000L;
    private static final long DECISION_DISPLAY_MILLIS = 1_500L;
    private static final long REMINDER_MILLIS = 10_000L;

    public static boolean active(EventState event) {
        return event.hasVoting();
    }

    public static void open(MinecraftServer server, EventState event, int wave) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { openInScope(server, event, wave); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> openInScope(server, store.events().bind(event), wave));
    }

    private static void openInScope(MinecraftServer server, EventState event, int wave) {
        EventVote voting = new EventVote();
        voting.wave(wave);
        voting.ends(System.currentTimeMillis() + TIMEOUT_MILLIS);
        voting.remindAt(System.currentTimeMillis()+REMINDER_MILLIS);
        voting.votes(java.util.Map.of());
        event.voting(voting);

        announce(server,event);
    }

    public static void vote(ServerPlayer player, UUID eventId, boolean continueBattle) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { voteInScope(player, eventId, continueBattle); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> voteInScope(player, eventId, continueBattle));
    }

    private static void voteInScope(ServerPlayer player, UUID eventId, boolean continueBattle) {
        EventState event = SmpData.get(player.server).events().current(eventId);
        if (event == null || !event.state().equals("ACTIVE") || !active(event)) return;
        EventVote voting = event.voting();
        if (voting.resolved() || participants(player.server, event).stream().noneMatch(value -> value.getUUID().equals(player.getUUID()))) return;
        var choices = new java.util.LinkedHashMap<>(voting.votes());
        choices.put(player.getUUID(), continueBattle ? CONTINUE : CASH_OUT);
        voting.votes(choices);
        event.voting(voting);

        evaluate(player.server, event);
        announce(player.server,event);
        CombatNetwork.refreshBloodMoon(player.server, event);
    }

    public static void tick(MinecraftServer server, EventState event) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { tickInScope(server, event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> tickInScope(server, store.events().bind(event)));
    }

    private static void tickInScope(MinecraftServer server, EventState event) {
        if (!active(event)) return;
        EventVote voting = event.voting();
        long now = System.currentTimeMillis();
        if (voting.resolved()) {
            if (now < voting.decisionAt()) return;
            if (CONTINUE.equals(voting.decision())) {
                event.continuedWave(voting.wave());
                event.clearVoting();
                event.nextWave(now);

                CombatNetwork.refreshBloodMoon(server, event);
            } else {
                BloodMoon.cashOut(server, event);
            }
            return;
        }
        if(now>=voting.remindAt()) {
            voting.remindAt(Long.MAX_VALUE);
            event.voting(voting);

            announce(server,event);
        }
        if (!evaluate(server, event) && now >= voting.ends()) resolve(server, event, CASH_OUT);
    }

    public static void disconnect(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { disconnectInScope(player); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> disconnectInScope(player));
    }

    private static void disconnectInScope(ServerPlayer player) {
        for (EventState event : SmpData.get(player.server).events().currentRows()) {
            if (!event.state().equals("ACTIVE") || !active(event)
                    || !new EventSession(event).accepted(player.getUUID())) continue;
            var voting = event.voting();
            var choices = new java.util.LinkedHashMap<>(voting.votes());
            choices.remove(player.getUUID());
            voting.votes(choices);
            List<ServerPlayer> remaining = participants(player.server, event).stream()
                    .filter(value -> !value.getUUID().equals(player.getUUID())).toList();
            evaluate(player.server, event, remaining);

            CombatNetwork.refreshBloodMoon(player.server, event);
        }
    }

    private static boolean evaluate(MinecraftServer server, EventState event) {
        return evaluate(server, event, participants(server, event));
    }

    private static boolean evaluate(MinecraftServer server, EventState event, List<ServerPlayer> participants) {
        EventVote voting = event.voting();
        int required = participants.size() / 2 + 1;
        int continueVotes = votes(voting, participants, CONTINUE);
        int cashOutVotes = votes(voting, participants, CASH_OUT);
        if (continueVotes >= required) {
            resolve(server, event, CONTINUE);
            return true;
        }
        if (cashOutVotes >= required) {
            resolve(server, event, CASH_OUT);
            return true;
        }
        return false;
    }

    private static void resolve(MinecraftServer server, EventState event, String decision) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { resolveInScope(server, event, decision); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> resolveInScope(server, store.events().bind(event), decision));
    }

    private static void resolveInScope(MinecraftServer server, EventState event, String decision) {
        EventVote voting = event.voting();
        if (voting.resolved()) return;
        voting.resolved(true);
        voting.decision(decision);
        voting.decisionAt(System.currentTimeMillis() + DECISION_DISPLAY_MILLIS);
        event.voting(voting);

        announce(server,event);
        CombatNetwork.refreshBloodMoon(server, event);
    }

    private static void announce(MinecraftServer server,EventState event) {
        if(!active(event)) return;
        var voting=event.voting();
        var participants=participants(server,event);
        var votes=voting.votes();
        for(var viewer:participants) {
            viewer.sendSystemMessage(Component.translatable("jem.smp.vote.title",voting.wave()).withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD));
            for(var participant:participants) {
                String vote=votes.getOrDefault(participant.getUUID(), "");
                ChatFormatting color=CONTINUE.equals(vote)?ChatFormatting.GREEN:CASH_OUT.equals(vote)?ChatFormatting.RED:ChatFormatting.GRAY;
                viewer.sendSystemMessage(Component.literal("в–Ј "+participant.getGameProfile().getName()+" вЂ” ")
                        .withStyle(color).append(Component.translatable(vote.isEmpty()?"jem.smp.vote.pending":CONTINUE.equals(vote)?"jem.smp.vote.continue_state":"jem.smp.vote.cash_out_state").withStyle(color)));
            }
            if(!voting.resolved()) {
                var message=Component.translatable("jem.smp.vote.continue_button").withStyle(style->style.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/smp vote "+event.id()+" continue")));
                message.append(Component.literal("   "));
                message.append(Component.translatable("jem.smp.vote.cashout_button").withStyle(style->style.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/smp vote "+event.id()+" cashout"))));
                viewer.sendSystemMessage(message);
            }
        }
    }

    public static void snapshot(MinecraftServer server, EventState event, CompoundTag target) {
        if (!active(event)) return;
        EventVote voting = event.voting();
        List<ServerPlayer> participants = participants(server, event);
        var votes = voting.votes();
        ListTag rows = new ListTag();
        for (ServerPlayer player : participants) {
            CompoundTag row = new CompoundTag();
            row.putUUID("id", player.getUUID());
            row.putString("name", player.getGameProfile().getName());
            row.putString("vote", votes.getOrDefault(player.getUUID(), ""));
            rows.add(row);
        }
        target.putBoolean("voting", true);
        target.putInt("voteWave", voting.wave());
        target.putLong("voteEnds", voting.ends());
        target.putInt("requiredVotes", participants.size() / 2 + 1);
        target.putInt("continueVotes", votes(voting, participants, CONTINUE));
        target.putInt("cashOutVotes", votes(voting, participants, CASH_OUT));
        target.putBoolean("voteResolved", voting.resolved());
        target.putString("voteDecision", voting.decision());
        target.put("voteRows", rows);
        if (voting.wave() < BloodMoonWaves.WAVES)
            target.put("nextReward", BloodMoonRewards.previewWave(voting.wave() + 1));
    }

    private static int votes(EventVote voting, List<ServerPlayer> participants, String choice) {
        var votes = voting.votes();
        return (int) participants.stream().filter(player -> choice.equals(votes.getOrDefault(player.getUUID(), ""))).count();
    }

    private static List<ServerPlayer> participants(MinecraftServer server, EventState event) {
        return new EventSession(event).active(server).stream().filter(player -> player.connection.isAcceptingMessages()).toList();
    }

    private BloodMoonVoting() {
    }
}
