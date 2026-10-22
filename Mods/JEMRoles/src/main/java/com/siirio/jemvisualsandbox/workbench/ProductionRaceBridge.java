package com.siirio.jemvisualsandbox.workbench;

import com.siirio.jemidentity.api.DedicatedTestProfile;
import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceRecord;
import com.siirio.jemroles.race.RaceSavedData;
import com.siirio.jemroles.race.RaceService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Final mechanics view of the sole identity-bound race record on every logical server. */
public final class ProductionRaceBridge {
    private ProductionRaceBridge() {}

    public static boolean authenticated(ServerPlayer player) {
        return player != null && IdentityPolicy.resolve(player).isPresent();
    }

    /** Boss membership remains owned by its optional profile registry. */
    public static boolean registeredBoss(net.minecraft.world.entity.LivingEntity entity) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers")) return false;
        try {
            return ((Optional<?>)Class.forName("com.siirio.jemworldbosstiers.api.WorldTierApi")
                    .getMethod("profile", net.minecraft.world.entity.LivingEntity.class).invoke(null, entity)).isPresent();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Registered boss profile API", failure);
        }
    }

    /** Optional encounter authority is shared by controls, healing and damage. */
    public static boolean canInteract(net.minecraft.world.entity.LivingEntity actor, net.minecraft.world.entity.LivingEntity target) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers")) return true;
        try {
            return (boolean)Class.forName("com.siirio.jemworldbosstiers.api.HostedEncounterApi")
                    .getMethod("canInteract", net.minecraft.world.entity.LivingEntity.class, net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, actor, target);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Hosted encounter interaction API", failure);
        }
    }

    public static ServerPlayer damageOwner(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        ServerPlayer guardian = SandboxUtilities.guardianOwner(entity);
        if (guardian != null) return guardian;
        if (entity instanceof net.minecraft.world.entity.TamableAnimal pet) return pet.getOwner() instanceof ServerPlayer player ? player : null;
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers")) return null;
        try {
            Object owner=Class.forName("com.siirio.jemworldbosstiers.api.HostedEncounterApi")
                    .getMethod("damageOwner", net.minecraft.world.entity.Entity.class).invoke(null, entity);
            return owner instanceof ServerPlayer player ? player : null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Hosted encounter owner API", failure);
        }
    }

    public static CompoundTag data(ServerPlayer player) {
        RaceRecord record = RaceService.record(player).orElse(null);
        if (record == null) return new CompoundTag();
        CompoundTag tag = record.mechanics();
        int race = record.race().ordinal();
        if (!tag.getBoolean("Migrated")) {
            tag.putInt("Uses" + race, record.spellProgress());
            for (String key : new String[]{"signature", "guardian", "gravity", "void_projectile", "lava_drink", "second_blossom"})
                tag.putLong("Cooldown" + race + "_" + key, record.cooldown(key));
            tag.putInt("BloomUpgrade", race == 2 ? RacePowerStages.stageForUses(tag.getInt("Uses2")) : 0);
            player.getPersistentData().putInt("JemSandboxResonance", record.resonance());
            tag.putBoolean("Migrated", true);
            dirty(player);
        }
        if (!tag.getBoolean("GuardianCooldownV2")) {
            long now = player.server.overworld().getGameTime();
            long maximum = now + com.siirio.jemroles.race.RaceRules.GUARDIAN_COOLDOWN.get();
            if (tag.getLong("Cooldown0_guardian") > maximum) tag.putLong("Cooldown0_guardian", maximum);
            tag.putBoolean("GuardianCooldownV2", true);
            dirty(player);
        }
        tag.putBoolean("Selected", true);
        tag.putInt("Race", race);
        tag.putLong("ActiveTicks", record.activeTicks());
        tag.putBoolean("FreeSwitchUsed", record.freeSwitchUsed());
        return tag;
    }

    public static void dirty(ServerPlayer player) { RaceSavedData.get(player.server).changed(); }
    public static long remainingReselectTicks(ServerPlayer player) {
        return RaceService.record(player).map(record -> record.remainingReselectTicks(player.server.overworld().getGameTime())).orElse(0L);
    }
    public static boolean testProfile(ServerPlayer player) { return DedicatedTestProfile.active(player.server); }
    public static UUID nonce(ServerPlayer player) { return RaceService.currentNonce(player); }
    public static String choose(ServerPlayer player, int race, UUID nonce) {
        RaceService.Result result = RaceService.choose(player, RaceId.values()[race], nonce);
        if (result == RaceService.Result.CHANGE_LOCKED)
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jem.roles.change_locked"), true);
        return result.name();
    }
    public static void tick(ServerPlayer player) { RaceService.mutate(player, record -> record.addActiveTicks(20L)); }
}
