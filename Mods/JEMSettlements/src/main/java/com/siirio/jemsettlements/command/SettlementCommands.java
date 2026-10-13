package com.siirio.jemsettlements.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.town.SettlementSavedData;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SettlementCommands {
    private SettlementCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (!DedicatedSmp.accepts(event)) return;
        event.getDispatcher().register(Commands.literal("jem")
                .then(Commands.literal("town")
                        .executes(context -> status(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> create(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("linkstorage")
                                .executes(context -> linkStorage(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("workforce")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(context -> workforce(context.getSource().getPlayerOrException(), EntityArgument.getEntity(context, "entity")))))));
    }

    private static int create(ServerPlayer player, String name) {
        SettlementApi.Result result = SettlementApi.create(player, name);
        switch (result) {
            case ACCEPTED -> player.sendSystemMessage(Component.translatable("jem.settlements.created", name));
            case UNAUTHENTICATED -> player.sendSystemMessage(Component.translatable("jem.settlements.identity_required"));
            case ALREADY_OWNED -> player.sendSystemMessage(Component.translatable("jem.settlements.exists"));
            default -> player.sendSystemMessage(Component.translatable("jem.settlements.invalid_name"));
        }
        return result == SettlementApi.Result.ACCEPTED ? 1 : 0;
    }

    private static int status(ServerPlayer player) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.settlements.identity_required"));
            return 0;
        }
        var town = SettlementApi.ownedTown(player);
        if (town.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.settlements.none"));
            return 0;
        }
        long workers = SettlementSavedData.get(player.getServer()).workforceCount(town.get().id());
        player.sendSystemMessage(Component.translatable("jem.settlements.status", town.get().name(), town.get().happiness(), workers, town.get().storage().size()));
        return 1;
    }

    private static int linkStorage(ServerPlayer player) {
        var hit = player.pick(6.0D, 0.0F, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) return 0;
        return SettlementApi.linkPhysicalStorage(player, blockHit.getBlockPos()) == SettlementApi.Result.ACCEPTED ? 1 : 0;
    }

    private static int workforce(ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        SettlementApi.Result result = SettlementApi.registerNativeWorker(player, entity,
                com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT, false);
        player.sendSystemMessage(Component.translatable(result == SettlementApi.Result.ACCEPTED
                ? "jem.settlements.workforce_added" : "jem.settlements.workforce_rejected"));
        return result == SettlementApi.Result.ACCEPTED ? 1 : 0;
    }
}
