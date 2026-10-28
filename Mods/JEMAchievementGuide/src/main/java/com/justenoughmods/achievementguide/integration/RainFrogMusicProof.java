package com.justenoughmods.achievementguide.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

/** Authenticated native disc insertion and a later validated native server frog response. */
public final class RainFrogMusicProof {
    private static final String KEY = "JemQuestMusicOperatorV4";
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("jem_advancements", "quest_v3/v3_fauna_small_surprises");
    public record Insertion(ServerPlayer actor, JukeboxBlockEntity jukebox, ItemStack source, ItemStack disc, int count) {}
    private RainFrogMusicProof() {}

    public static Insertion before(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer actor) || actor.isSpectator() || actor.getAbilities().instabuild
                || actor instanceof net.minecraftforge.common.util.FakePlayer || context.getLevel() != actor.level()
                || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof JukeboxBlockEntity jukebox)
                || !jukebox.getFirstItem().isEmpty() || context.getItemInHand().isEmpty()) return null;
        var source = context.getItemInHand();
        return new Insertion(actor, jukebox, source, source.copyWithCount(1), source.getCount());
    }

    public static void inserted(Insertion before, boolean accepted) {
        if (before == null || !accepted || before.count - before.source.getCount() != 1 || before.jukebox.isRemoved()
                || before.jukebox.getLevel() != before.actor.level()
                || before.actor.level().getBlockEntity(before.jukebox.getBlockPos()) != before.jukebox || !before.jukebox.isRecordPlaying()
                || !ItemStack.matches(before.disc, before.jukebox.getFirstItem())) return;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", before.actor.getUUID());
        proof.put("disc", before.disc.save(new CompoundTag()));
        before.jukebox.getPersistentData().put(KEY, proof);
        before.jukebox.setChanged();
    }

    public static void invalidated(JukeboxBlockEntity jukebox) {
        if (jukebox.getLevel() instanceof ServerLevel && jukebox.getPersistentData().contains(KEY)) {
            jukebox.getPersistentData().remove(KEY); jukebox.setChanged();
        }
    }

    public static void reacted(JukeboxBlockEntity jukebox) {
        if (!(jukebox.getLevel() instanceof ServerLevel level) || jukebox.isRemoved() || !jukebox.isRecordPlaying()) return;
        var proof = jukebox.getPersistentData().getCompound(KEY);
        if (!proof.hasUUID("actor") || proof.getBoolean("reaction")
                || !ItemStack.matches(ItemStack.of(proof.getCompound("disc")), jukebox.getFirstItem())) return;
        NativeQuestRecipients.accepted(level.getServer(), proof.getUUID("actor"), "v3_fauna_small_surprises", ADVANCEMENT);
        proof.putBoolean("reaction", true);
        jukebox.setChanged();
    }
}
