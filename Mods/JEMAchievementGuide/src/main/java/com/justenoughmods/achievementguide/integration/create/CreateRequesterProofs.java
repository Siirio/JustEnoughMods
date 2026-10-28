package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

/** Last successful native configuration commit on the actual requester. */
public final class CreateRequesterProofs {
    private static final String KEY = "jemQuestRequesterV1";
    private CreateRequesterProofs() {}
    public static CompoundTag configuration(RedstoneRequesterBlockEntity owner) {
        CompoundTag tag = new CompoundTag(); owner.writeSafe(tag); return tag;
    }
    public static void configured(RedstoneRequesterBlockEntity owner, ServerPlayer actor, CompoundTag before) {
        CompoundTag after = configuration(owner);
        if (actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild || before.equals(after)) return;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", actor.getUUID()); proof.put("configuration", after);
        owner.getPersistentData().put(KEY, proof); owner.setChanged();
    }
    public static ServerPlayer actor(RedstoneRequesterBlockEntity owner) {
        CompoundTag proof = owner.getPersistentData().getCompound(KEY);
        if (!(owner.getLevel() instanceof ServerLevel level) || !proof.hasUUID("actor")
                || !configuration(owner).equals(proof.getCompound("configuration"))) return null;
        return level.getServer().getPlayerList().getPlayer(proof.getUUID("actor"));
    }
}
