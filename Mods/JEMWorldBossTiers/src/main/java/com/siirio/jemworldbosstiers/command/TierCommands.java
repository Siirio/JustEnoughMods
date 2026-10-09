package com.siirio.jemworldbosstiers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.encounter.EncounterData;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class TierCommands {
    private static final String ADMINISTRATOR_NAME = "Sirio_o";

    private TierCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jemtier")
                .requires(TierCommands::administrator)
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("set").then(Commands.argument("tier",IntegerArgumentType.integer(1,5))
                        .executes(context -> set(context.getSource(),IntegerArgumentType.getInteger(context,"tier")))))
                .then(Commands.literal("reset").executes(context -> reset(context.getSource())))
                .then(Commands.literal("grant").then(Commands.argument("profile", ResourceLocationArgument.id()).executes(context -> grant(context.getSource(), ResourceLocationArgument.getId(context, "profile")))))
                .then(Commands.literal("boss").then(Commands.argument("entity", EntityArgument.entity()).executes(context -> boss(context.getSource(), EntityArgument.getEntity(context, "entity")))))
                .then(Commands.literal("arena").executes(context -> arena(context.getSource()))));
    }

    private static boolean administrator(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                && player.getGameProfile().getName().equalsIgnoreCase(ADMINISTRATOR_NAME);
    }

    private static int grant(CommandSourceStack source, ResourceLocation profileId) {
        BossProfile profile = BalanceRegistry.bossByKey(profileId).orElse(null);
        if (profile == null) {
            source.sendFailure(Component.literal("No boss profile for " + profileId));
            return 0;
        }
        com.siirio.jemworldbosstiers.JemWorldBossTiers.recordVictory(source.getServer(), profile);
        source.sendSuccess(() -> Component.literal("Recorded victory over " + profile.displayName()), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        WorldTierData data = WorldTierData.get(source.getServer());
        int count = data.activeDefeatCount();
        int next = BalanceRegistry.tierThresholds().stream().filter(threshold -> threshold > count).findFirst().orElse(-1);
        String defeated = data.defeatedBosses().stream().filter(BalanceRegistry.activeQualifyingKeys()::contains).sorted(Comparator.comparing(ResourceLocation::toString)).map(ResourceLocation::toString).collect(Collectors.joining(", "));
        String sourceName=data.tierOverride().isPresent()?"manual":"progression";
        source.sendSuccess(() -> Component.literal("World Tier " + data.tier() + " | source " + sourceName + " | qualifying defeats " + count + " | next " + (next < 0 ? "MAX" : next) + " | defeated " + (defeated.isEmpty() ? "none" : defeated)), false);
        return data.tier();
    }

    private static int set(CommandSourceStack source,int tier) {
        int applied=com.siirio.jemworldbosstiers.JemWorldBossTiers.overrideWorldTier(source.getServer(),tier);
        source.sendSuccess(()->Component.literal("World Tier set to "+applied),true);
        return applied;
    }

    private static int reset(CommandSourceStack source) {
        int applied=com.siirio.jemworldbosstiers.JemWorldBossTiers.resetWorldTier(source.getServer());
        source.sendSuccess(()->Component.literal("World Tier reset to progression value "+applied),true);
        return applied;
    }

    private static int boss(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Entity is not living"));
            return 0;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        BossProfile profile = BalanceRegistry.boss(entityId).orElse(null);
        if (profile == null) {
            source.sendFailure(Component.literal("No boss profile for " + entityId));
            return 0;
        }
        EncounterData encounter = EncounterData.read(living.getPersistentData()).orElse(null);
        int tier = encounter == null ? WorldTierData.get(source.getServer()).tier() : encounter.tier();
        String state = encounter == null ? "uninitialized" : "tier=" + encounter.tier() + ", provenance=" + encounter.provenance() + ", rematch=" + encounter.rematch() + ", eligible=" + encounter.canProgressUnique();
        String pressure = profile.specialDamageMultiplier() == null ? "native" : Double.toString(profile.specialDamageMultiplier().valueAt(tier));
        source.sendSuccess(() -> Component.literal(profile.displayName() + " | profile=" + profile.key() + " | nativeThreat=" + profile.nativeThreat() + " | scales=" + profile.scalesWithWorldTier() + " | counts=" + profile.countsTowardWorldTier() + " | damageAdapter=" + profile.damageAdapter() + " | healingAdapter=" + profile.healingAdapter() + " | staggerAdapter=" + profile.staggerAdapter() + " | antiCheese=" + profile.antiCheeseProfile() + " | destruction=" + profile.destructionRules() + " | specialPressureTarget=" + pressure + " | " + state), false);
        return 1;
    }

    private static int arena(CommandSourceStack source) {
        String dimension = source.getLevel().dimension().location().toString();
        ArenaRecord arena = WorldTierData.get(source.getServer()).arenas().stream().filter(value -> value.contains(dimension, source.getPosition().x, source.getPosition().y, source.getPosition().z)).findFirst().orElse(null);
        if (arena == null) {
            source.sendFailure(Component.literal("No registered arena at this position"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Arena " + arena.id() + " | profile=" + arena.profileKey() + " | unlocked=" + arena.unlocked() + " | active=" + (arena.activeEncounterId() == null ? "none" : arena.activeEncounterId())), false);
        return 1;
    }
}
