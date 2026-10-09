package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.CombatNetwork;
import com.siirio.jemserver.smp.SmpData;
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
    private static final String KEY = "voting";
    private static final String CONTINUE = "CONTINUE";
    private static final String CASH_OUT = "CASH_OUT";
    private static final long TIMEOUT_MILLIS = 20_000L;
    private static final long DECISION_DISPLAY_MILLIS = 1_500L;
    private static final long REMINDER_MILLIS = 10_000L;

    public static boolean active(CompoundTag event) {
        return event.contains(KEY);
    }

    public static void open(MinecraftServer server, CompoundTag event, int wave) {
        CompoundTag voting = new CompoundTag();
        voting.putInt("wave", wave);
        voting.putLong("ends", System.currentTimeMillis() + TIMEOUT_MILLIS);
        voting.putLong("remindAt",System.currentTimeMillis()+REMINDER_MILLIS);
        voting.put("votes", new CompoundTag());
        event.put(KEY, voting);
        SmpData.get(server).changed(event);
        announce(server,event);
    }

    public static void vote(ServerPlayer player, UUID eventId, boolean continueBattle) {
        CompoundTag event = SmpData.get(player.server).find("events", eventId);
        if (event == null || !event.getString("state").equals("ACTIVE") || !active(event)) return;
        CompoundTag voting = event.getCompound(KEY);
        if (voting.getBoolean("resolved") || participants(player.server, event).stream().noneMatch(value -> value.getUUID().equals(player.getUUID()))) return;
        voting.getCompound("votes").putString(player.getStringUUID(), continueBattle ? CONTINUE : CASH_OUT);
        event.put(KEY, voting);
        SmpData.get(player.server).changed(event);
        evaluate(player.server, event);
        announce(player.server,event);
        CombatNetwork.refreshBloodMoon(player.server, event);
    }

    public static void tick(MinecraftServer server, CompoundTag event) {
        if (!active(event)) return;
        CompoundTag voting = event.getCompound(KEY);
        long now = System.currentTimeMillis();
        if (voting.getBoolean("resolved")) {
            if (now < voting.getLong("decisionAt")) return;
            if (CONTINUE.equals(voting.getString("decision"))) {
                event.putInt("continuedWave",voting.getInt("wave"));
                event.remove(KEY);
                event.putLong("nextWave", now);
                SmpData.get(server).changed(event);
                CombatNetwork.refreshBloodMoon(server, event);
            } else {
                BloodMoon.cashOut(server, event);
            }
            return;
        }
        if(now>=voting.getLong("remindAt")) {
            voting.putLong("remindAt",Long.MAX_VALUE);
            event.put(KEY,voting);
            SmpData.get(server).changed(event);
            announce(server,event);
        }
        if (!evaluate(server, event) && now >= voting.getLong("ends")) resolve(server, event, CASH_OUT);
    }

    public static void disconnect(ServerPlayer player) {
        for (CompoundTag event : SmpData.get(player.server).all("events")) {
            if (!event.getString("state").equals("ACTIVE") || !active(event)
                    || !new EventSession(event).accepted(player.getUUID())) continue;
            event.getCompound(KEY).getCompound("votes").remove(player.getStringUUID());
            List<ServerPlayer> remaining = participants(player.server, event).stream()
                    .filter(value -> !value.getUUID().equals(player.getUUID())).toList();
            evaluate(player.server, event, remaining);
            SmpData.get(player.server).changed(event);
            CombatNetwork.refreshBloodMoon(player.server, event);
        }
    }

    private static boolean evaluate(MinecraftServer server, CompoundTag event) {
        return evaluate(server, event, participants(server, event));
    }

    private static boolean evaluate(MinecraftServer server, CompoundTag event, List<ServerPlayer> participants) {
        CompoundTag voting = event.getCompound(KEY);
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

    private static void resolve(MinecraftServer server, CompoundTag event, String decision) {
        CompoundTag voting = event.getCompound(KEY);
        if (voting.getBoolean("resolved")) return;
        voting.putBoolean("resolved", true);
        voting.putString("decision", decision);
        voting.putLong("decisionAt", System.currentTimeMillis() + DECISION_DISPLAY_MILLIS);
        event.put(KEY, voting);
        SmpData.get(server).changed(event);
        announce(server,event);
        CombatNetwork.refreshBloodMoon(server, event);
    }

    private static void announce(MinecraftServer server,CompoundTag event) {
        if(!active(event)) return;
        var voting=event.getCompound(KEY);
        var participants=participants(server,event);
        var votes=voting.getCompound("votes");
        for(var viewer:participants) {
            viewer.sendSystemMessage(Component.translatable("jem.smp.vote.title",voting.getInt("wave")).withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD));
            for(var participant:participants) {
                String vote=votes.getString(participant.getStringUUID());
                ChatFormatting color=CONTINUE.equals(vote)?ChatFormatting.GREEN:CASH_OUT.equals(vote)?ChatFormatting.RED:ChatFormatting.GRAY;
                viewer.sendSystemMessage(Component.literal("▣ "+participant.getGameProfile().getName()+" — ")
                        .withStyle(color).append(Component.translatable(vote.isEmpty()?"jem.smp.vote.pending":CONTINUE.equals(vote)?"jem.smp.vote.continue_state":"jem.smp.vote.cash_out_state").withStyle(color)));
            }
            if(!voting.getBoolean("resolved")) {
                var message=Component.literal("[Продолжить]").withStyle(style->style.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/smp vote "+event.getUUID("id")+" continue")));
                message.append(Component.literal("   "));
                message.append(Component.literal("[Забрать награду]").withStyle(style->style.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/smp vote "+event.getUUID("id")+" cashout"))));
                viewer.sendSystemMessage(message);
            }
        }
    }

    public static void snapshot(MinecraftServer server, CompoundTag event, CompoundTag target) {
        if (!active(event)) return;
        CompoundTag voting = event.getCompound(KEY);
        List<ServerPlayer> participants = participants(server, event);
        CompoundTag votes = voting.getCompound("votes");
        ListTag rows = new ListTag();
        for (ServerPlayer player : participants) {
            CompoundTag row = new CompoundTag();
            row.putUUID("id", player.getUUID());
            row.putString("name", player.getGameProfile().getName());
            row.putString("vote", votes.getString(player.getStringUUID()));
            rows.add(row);
        }
        target.putBoolean("voting", true);
        target.putInt("voteWave", voting.getInt("wave"));
        target.putLong("voteEnds", voting.getLong("ends"));
        target.putInt("requiredVotes", participants.size() / 2 + 1);
        target.putInt("continueVotes", votes(voting, participants, CONTINUE));
        target.putInt("cashOutVotes", votes(voting, participants, CASH_OUT));
        target.putBoolean("voteResolved", voting.getBoolean("resolved"));
        target.putString("voteDecision", voting.getString("decision"));
        target.put("voteRows", rows);
        if (voting.getInt("wave") < BloodMoonWaves.WAVES)
            target.put("nextReward", BloodMoonRewards.previewWave(voting.getInt("wave") + 1));
    }

    private static int votes(CompoundTag voting, List<ServerPlayer> participants, String choice) {
        CompoundTag votes = voting.getCompound("votes");
        return (int) participants.stream().filter(player -> choice.equals(votes.getString(player.getStringUUID()))).count();
    }

    private static List<ServerPlayer> participants(MinecraftServer server, CompoundTag event) {
        return new EventSession(event).active(server).stream().filter(player -> player.connection.isAcceptingMessages()).toList();
    }

    private BloodMoonVoting() {
    }
}
