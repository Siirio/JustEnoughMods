package com.siirio.jemworldbosstiers.encounter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Transaction ordering inside the existing encounter and reward owners. */
final class HostedSettlement {
    private HostedSettlement() {}

    static void finish(Runnable persistIntent, Runnable persistRewards, Runnable terminal, Runnable complete) {
        persistIntent.run();
        persistRewards.run();
        terminal.run();
        complete.run();
    }

    static boolean start(java.util.function.BooleanSupplier persistAttempt, Runnable persistParty, Runnable release) {
        if (!persistAttempt.getAsBoolean()) return false;
        persistParty.run();
        release.run();
        return true;
    }

    static Set<UUID> recipients(CompoundTag state, double minimum) {
        Set<UUID> result = new LinkedHashSet<>();
        if (state.contains("FrozenRecipients", Tag.TAG_LIST)) {
            for (Tag id : state.getList("FrozenRecipients", Tag.TAG_STRING)) result.add(UUID.fromString(id.getAsString()));
            return Set.copyOf(result);
        }
        CompoundTag players = state.getCompound("Players");
        double startingHealth = state.getDouble("StartingHealth");
        for (String id : players.getAllKeys()) {
            CompoundTag entry = players.getCompound(id);
            double amount = entry.getDouble("Contribution");
            if (entry.getBoolean("Supported") || startingHealth > 0 && Double.isFinite(amount)
                    && amount > 0 && amount >= startingHealth * minimum) result.add(UUID.fromString(id));
        }
        return Set.copyOf(result);
    }

    static void freezeRecipients(CompoundTag state, double minimum) {
        if (state.contains("FrozenRecipients", Tag.TAG_LIST)) return;
        ListTag ids = new ListTag();
        recipients(state, minimum).stream().sorted().forEach(id -> ids.add(StringTag.valueOf(id.toString())));
        state.put("FrozenRecipients", ids);
    }

    static void eliminate(CompoundTag participant, boolean nativeDeath) {
        participant.putString("State", "ELIMINATED");
        if (nativeDeath) participant.putBoolean("NativeDeath", true);
    }

    static void mergeEliminations(CompoundTag state, CompoundTag reconciled) {
        CompoundTag players = state.getCompound("Players"), merged = reconciled.getCompound("Players");
        for (String id : players.getAllKeys()) {
            CompoundTag participant = players.getCompound(id), canonical = merged.getCompound(id);
            if ("ELIMINATED".equals(canonical.getString("State"))) eliminate(participant, canonical.getBoolean("NativeDeath"));
            else if (canonical.getBoolean("NativeDeath")) eliminate(participant, true);
        }
    }

    static boolean supportEligible(CompoundTag state, UUID helper, UUID target) {
        if (helper.equals(target) || state.contains("VictoryAt") || state.getBoolean("FailureClosing") || state.getBoolean("LinkPending")) return false;
        CompoundTag players = state.getCompound("Players");
        if (!players.contains(helper.toString()) || !players.contains(target.toString())) return false;
        CompoundTag source = players.getCompound(helper.toString()), recipient = players.getCompound(target.toString());
        double contribution = recipient.getDouble("Contribution");
        return "ALIVE".equals(source.getString("State")) && "ALIVE".equals(recipient.getString("State"))
                && Double.isFinite(contribution) && contribution > 0;
    }

    static boolean expireAbsence(CompoundTag participant, long now) {
        if ("ELIMINATED".equals(participant.getString("State")) || !participant.contains("DisconnectUntil")
                || now < participant.getLong("DisconnectUntil")) return false;
        participant.putBoolean("AbsenceExpired", true);
        eliminate(participant, false);
        return true;
    }
}
