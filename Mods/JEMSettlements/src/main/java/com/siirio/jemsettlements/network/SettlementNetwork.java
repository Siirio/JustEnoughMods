package com.siirio.jemsettlements.network;

import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SettlementNetwork {
    private static final String VERSION = "9";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JemSettlements.MOD_ID, "town"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(value -> VERSION.equals(value) || NetworkRegistry.ABSENT.equals(value) || NetworkRegistry.ACCEPTVANILLA.equals(value)).serverAcceptedVersions(value -> VERSION.equals(value) || NetworkRegistry.ABSENT.equals(value) || NetworkRegistry.ACCEPTVANILLA.equals(value)).simpleChannel();
    private SettlementNetwork() {}

    static SimpleChannel channel() { return CHANNEL; }
    public static boolean supported() { return ClientConnection.remote(CHANNEL); }
    public static void requestOpen() { action("open", null, new CompoundTag(), java.util.UUID.randomUUID()); }
    public static void action(String action, java.util.UUID townId, CompoundTag payload, java.util.UUID requestId) {
        if (supported()) {
            CompoundTag intent = payload.copy(); ClientConnection.bind(intent);
            CHANNEL.sendToServer(new SettlementActionPacket(action, townId, requestId, intent));
        }
    }
    public static void acceptSession(CompoundTag view) { ClientConnection.accept(view); }
    public static void clearClientSession() { ClientConnection.clear(); }
    public static java.util.UUID newRequestId() { return ClientConnection.request(); }
    public static void sendStorageLabels(ServerPlayer player,CompoundTag view) { if(CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new StorageLabelsPacket(view)); }
    public static void register() {
        CHANNEL.messageBuilder(StorageLabelsPacket.class,7,NetworkDirection.PLAY_TO_CLIENT).encoder(StorageLabelsPacket::encode).decoder(StorageLabelsPacket::decode).consumerMainThread((packet,context)->{ DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemsettlements.client.StorageLabels.accept(packet.view()));context.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(SettlementActionPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SettlementActionPacket::encode).decoder(SettlementActionPacket::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) SettlementActions.handle(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(NobleQuotePacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NobleQuotePacket::encode).decoder(NobleQuotePacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> acceptNoble(packet.view()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(HireQuotePacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HireQuotePacket::encode).decoder(HireQuotePacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> acceptHire(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(TownFoundPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TownFoundPacket::encode).decoder(TownFoundPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && SettlementAuthority.active(player)) {
                        SettlementApi.Result result = SettlementApi.create(player, packet.name(), packet.bell());
                        if (result == SettlementApi.Result.ACCEPTED) com.siirio.jemsettlements.runtime.SettlementRuntime.celebrateFounding(player.serverLevel(), packet.bell());
                        player.sendSystemMessage(Component.translatable(result == SettlementApi.Result.ACCEPTED
                                ? "jem.settlements.created" : "jem.settlements.found_failed", packet.name()), true);
                        sendOwned(player, true);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(FoodPolicyPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FoodPolicyPacket::encode).decoder(FoodPolicyPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemsettlements.client.SettlementClientState.acceptFoodPolicy(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(TownViewPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TownViewPacket::encode).decoder(TownViewPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemsettlements.client.SettlementClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(TreasuryActionPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TreasuryActionPacket::encode).decoder(TreasuryActionPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && SettlementAuthority.active(player)) {
                        SettlementApi.Result result = SettlementApi.treasury(player, packet.deposit(), packet.amount(), packet.requestId());
                        player.sendSystemMessage(Component.translatable("jem.settlements.treasury." + result.name().toLowerCase(java.util.Locale.ROOT), com.siirio.jemsettlements.currency.SettlementCurrency.format(packet.amount())), true);
                        sendOwned(player, true);
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void sendNobleQuote(ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("recruits"))
            RecruitQuoteNetwork.sendNobleQuote(player, entity);
    }
    public static void sendHireQuote(ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("recruits"))
            RecruitQuoteNetwork.sendHireQuote(player, entity);
    }
    public static CompoundTag personalRecruitView(ServerPlayer player) {
        CompoundTag view = new CompoundTag(); view.putBoolean("CoinCurrency", com.siirio.jemsettlements.currency.SettlementCurrency.coins()); ListTag rows = new ListTag();
        var principal = com.siirio.jemidentity.api.IdentityPolicy.resolve(player);
        if (principal.isEmpty()) { view.put("Defense", rows); return view; }
        var data = SettlementSavedData.get(player.getServer()); var owner = principal.get().accountId();
        for (var record : data.workforce(owner)) {
            if (record.type() != com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT) continue;
            CompoundTag row = new CompoundTag(); row.putUUID("Id", record.entityId()); row.putString("NativeType", record.nativeEntityType().toString());
            SettlementNames.project(row, null, record.displayName(), SettlementNames.entityKey(record.nativeEntityType())); row.putBoolean("Living", record.living());
            row.putString("Loyalty", record.loyalty().name()); row.putInt("MissedPayrolls", record.missedPayrolls());
            row.putLong("Wage", com.siirio.jemsettlements.payroll.RecruitRank.of(record.nativeEntityType()).wage());
            row.putLong("HireCost", com.siirio.jemsettlements.payroll.RecruitRank.of(record.nativeEntityType()).hiringCost());
            row.putLong("NextPayroll", record.nextWageGameTime()); row.putBoolean("Ready", record.settlementActive());
            for (ServerLevel level : player.getServer().getAllLevels()) { var entity = level.getEntity(record.entityId());
                if (entity != null) { SettlementNames.project(row, entity, record.displayName(), SettlementNames.entityKey(record.nativeEntityType())); break; } }
            rows.add(row);
        }
        view.put("Defense", rows); view.putLong("UnpaidPayroll", data.employer(owner).payroll().stream().filter(period -> period.currentCurrency() && !period.paid()).mapToLong(com.siirio.jemsettlements.payroll.PayrollPeriod::total).sum());
        return view;
    }

    public static void found(String name, net.minecraft.core.BlockPos bell) { CHANNEL.sendToServer(new TownFoundPacket(name, bell)); }
    public static void treasury(boolean deposit, long amount) { CHANNEL.sendToServer(new TreasuryActionPacket(deposit, amount, java.util.UUID.randomUUID())); }
    public static void sendFoodPolicy(ServerPlayer player) { send(player, FoodPolicyPacket.snapshot()); }

    public static void sendFounding(ServerPlayer player, net.minecraft.core.BlockPos bell) {
        SettlementActions.founding(player, bell);
    }

    public static void sendOwned(ServerPlayer player, boolean open) { SettlementActions.open(player, open); }
    public static void sendSelected(ServerPlayer player, java.util.UUID townId, boolean open) { SettlementActions.select(player, townId, open); }
    public static void sendGuide(ServerPlayer player) {
        CompoundTag view = new CompoundTag(); view.putBoolean("Guide", true); send(player, new TownViewPacket(true, view));
    }
    private static void acceptNoble(CompoundTag view) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("recruits")) com.siirio.jemsettlements.client.NobleQuoteClient.accept(view);
    }
    private static void acceptHire(HireQuotePacket packet) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("recruits")) com.siirio.jemsettlements.client.HireQuoteClient.accept(packet.entityId(), packet.cost(), packet.wage(), packet.coins(), packet.affordable(), packet.reason(), packet.stats());
    }

    /** Bounded server-authorized projection for read-only consumers such as the SMP information screen. */
    public static CompoundTag readOnlyView(ServerPlayer viewer, TownRecord town) {
        CompoundTag view = buildView(viewer, town);
        boolean owner = com.siirio.jemidentity.api.IdentityPolicy.resolve(viewer).map(value -> town.owner().equals(value.accountId())).orElse(false);
        view.remove("Center");
        view.remove("Storage"); view.remove("Transactions"); view.remove("Territories"); view.remove("PublicTerritories"); view.remove("PlayerPosition"); view.remove("Owner");
        view.remove("Wallet"); view.remove("PhysicalCoins"); view.remove("Spendable");
        for (String list : java.util.List.of("Residents", "WorkforceRows", "Defense")) {
            var rows = view.getList(list, net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < rows.size(); i++) { var row = rows.getCompound(i); row.remove("Position"); row.remove("HomePos"); row.remove("WorkPos"); row.remove("EntityId"); }
        }
        view.remove("TerritoryMinX"); view.remove("TerritoryMaxX"); view.remove("TerritoryMinZ"); view.remove("TerritoryMaxZ");
        if (!owner) {
            view.remove("Residents"); view.remove("WorkforceRows");
            view.remove("Treasury"); view.remove("Tax"); view.remove("Stock"); view.remove("Production"); view.remove("SimulationJournal");
            view.remove("FoodUnits"); view.remove("LoadedStorage"); view.remove("PendingNotSpendable"); view.remove("FailedQuarantined");
            view.remove("PayrollShortfall"); view.remove("UnpaidPayroll");
        }
        return view;
    }

    static CompoundTag buildView(ServerPlayer player, TownRecord town) {
        CompoundTag view = new CompoundTag(); view.putBoolean("CoinCurrency", com.siirio.jemsettlements.currency.SettlementCurrency.coins()); view.putLong("ExpansionCostPerBlock", com.siirio.jemsettlements.payroll.SettlementEconomyConfig.expansionPerBlock()); view.putLong("ExpansionMinimumCost", com.siirio.jemsettlements.payroll.SettlementEconomyConfig.expansionMinimum()); view.putUUID("Id", town.id()); view.putString("Name", town.name());
        view.putInt("Happiness", town.happiness()); view.putInt("Tax", town.taxPercent()); view.putLong("Center", town.center().asLong());
        view.putInt("TerritoryRadius", town.territoryRadius()); view.putInt("TerritoryMinX", town.territoryMinX()); view.putInt("TerritoryMaxX", town.territoryMaxX()); view.putInt("TerritoryMinZ", town.territoryMinZ()); view.putInt("TerritoryMaxZ", town.territoryMaxZ()); view.putLong("TerritoryArea", town.territoryArea());
        view.putString("Tier", town.tier().name()); view.putString("PrimarySpecialization", town.primarySpecialization().name()); view.putString("SecondarySpecialization", town.secondarySpecialization().name());
        view.put("Metrics", town.metrics());
        view.putInt("Housing", town.housingScore()); view.putInt("Food", town.foodScore()); view.putInt("Safety", town.safetyScore()); view.putInt("Jobs", town.jobsScore()); view.putInt("Community", town.communityScore()); view.putInt("Decoration", town.decorationScore());
        view.putLong("Treasury", SettlementApi.treasuryBalance(player, town));
        var bulletin = com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? com.siirio.jemeconomy.api.EconomyApi.marketBulletin(player) : new com.siirio.jemeconomy.api.EconomyApi.MarketBulletin("", "", 0L, 0L, 0L);
        view.putString("MarketPrimary", bulletin.primaryGroup()); view.putString("MarketSecondary", bulletin.secondaryGroup());
        view.putLong("MarketTicks", bulletin.ticksUntilUpdate()); view.putLong("MarketAccountRemaining", bulletin.accountRemaining()); view.putLong("MarketGlobalRemaining", bulletin.globalRemaining());
        view.putString("Dimension", town.dimension().toString()); view.putInt("StorageLinks", town.storage().size());
        var data = SettlementSavedData.get(player.getServer());
        CompoundTag workforce = new CompoundTag();
        data.workforce(town.id()).stream().filter(com.siirio.jemsettlements.workforce.WorkforceRecord::settlementActive)
                .forEach(record -> workforce.putInt(record.type().name(), workforce.getInt(record.type().name()) + 1));
        view.put("Workforce", workforce);
        ListTag residents = new ListTag();
        for (var resident : data.residents(town.id()).stream().filter(com.siirio.jemsettlements.resident.ResidentMood::living).toList()) {
            CompoundTag row = new CompoundTag(); row.putUUID("Id", resident.villagerId()); row.putInt("Mood", resident.mood()); row.putInt("Satiety", resident.satiety()); row.putBoolean("Craving", resident.craving()); row.putBoolean("Living", resident.living());
            row.putInt("PersonalMood", resident.personalMood()); row.putInt("EnvironmentBonus", resident.environmentBonus()); row.putInt("SocialMoodCap", resident.socialMoodCap());
            row.putBoolean("Child", resident.child()); row.putString("EmploymentReason", town.metrics().getCompound("EmploymentReasons").getString(resident.villagerId().toString()));
            row.putBoolean("Home", resident.home().isPresent()); row.putBoolean("Work", row.getString("EmploymentReason").equals("employed")); row.putInt("HomeAmenities", resident.homeAmenities().points()); row.putInt("WorkAmenities", resident.workAmenities().points());
            row.putBoolean("AmenitiesStale", resident.homeAmenities().stale() || resident.workAmenities().stale());
            var residentLevel = player.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, town.dimension()));
            var entity = residentLevel == null ? null : residentLevel.getEntity(resident.villagerId());
            SettlementNames.project(row, entity, resident.displayName(), "entity.minecraft.villager");
            row.putString("Profession", resident.profession());
            if (entity instanceof net.minecraft.world.entity.npc.Villager villager) row.putString("Profession", net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString());
            resident.requestedDish().ifPresent(value -> row.putString("RequestedDish", value.toString())); residents.add(row);
        }
        view.put("Residents", residents);
        ListTag workforceRows = new ListTag();
        for (var record : data.workforce(town.id())) {
            CompoundTag row = new CompoundTag(); row.putUUID("Id", record.entityId()); row.putString("Type", record.type().name()); row.putBoolean("Living", record.living());
            row.putLong("Wage", com.siirio.jemsettlements.payroll.RecruitRank.of(record.nativeEntityType()).wage()); row.putLong("HireCost", com.siirio.jemsettlements.payroll.RecruitRank.of(record.nativeEntityType()).hiringCost()); row.putLong("NextPayroll", record.nextWageGameTime()); row.putString("Loyalty", record.loyalty().name()); row.putInt("MissedPayrolls", record.missedPayrolls());
            SettlementNames.project(row, null, record.displayName(), SettlementNames.entityKey(record.nativeEntityType()));
            row.putString("NativeType", record.nativeEntityType().toString()); row.putBoolean("NeedsEnrolled", record.needsEnrolled());
            for (ServerLevel candidate : player.getServer().getAllLevels()) { var entity = candidate.getEntity(record.entityId()); if (entity != null) { SettlementNames.project(row, entity, record.displayName(), SettlementNames.entityKey(record.nativeEntityType())); break; } }
            workforceRows.add(row);
        }
        view.put("WorkforceRows", workforceRows);
        ListTag defense = new ListTag(); long projected = 0L;
        for (var record : data.workforce(town.id())) if (record.type() == com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT) {
            long wage = com.siirio.jemsettlements.payroll.RecruitRank.of(record.nativeEntityType()).wage(); if (record.settlementActive()) projected += wage; CompoundTag row = new CompoundTag(); row.putUUID("Id", record.entityId()); row.putString("NativeType", record.nativeEntityType().toString()); row.putLong("Wage", wage); row.putLong("HireCost", com.siirio.jemsettlements.payroll.RecruitRank.of(record.nativeEntityType()).hiringCost()); row.putLong("NextPayroll", record.nextWageGameTime()); row.putString("Loyalty", record.loyalty().name()); row.putInt("MissedPayrolls", record.missedPayrolls()); row.putLong("LastPaidPeriod", record.lastPaidPeriod()); row.putBoolean("Living", record.living()); row.putBoolean("Ready", record.settlementActive() && town.consecutiveMissedPayrolls() < 2);
            for (ServerLevel candidate : player.getServer().getAllLevels()) { var entity = candidate.getEntity(record.entityId()); if (entity == null || !net.minecraftforge.fml.ModList.get().isLoaded("recruits")) continue; var readiness = com.siirio.jemsettlements.compat.recruits.RecruitReadinessAdapter.read(entity); if (readiness.isPresent()) { row.putFloat("Morale", readiness.get().morale()); row.putFloat("Hunger", readiness.get().hunger()); row.putBoolean("Ready", row.getBoolean("Ready") && readiness.get().combatReady()); SettlementNames.project(row, entity, record.displayName(), SettlementNames.entityKey(record.nativeEntityType())); break; } }
            if (!row.contains("Name")) SettlementNames.project(row, null, record.displayName(), SettlementNames.entityKey(record.nativeEntityType()));
            row.putString("Type", "RECRUIT"); row.putString("Kind", "RECRUIT"); row.putBoolean("NeedsEnrolled", record.needsEnrolled());
            if (record.needsEnrolled()) {
                var history = record.foodHistory();
                SettlementViewDetails.foodCounts(row,history,player.getServer().overworld().getGameTime());
                if (row.contains("Morale")) {
                    row.putBoolean("NeedsKnown", true); row.putInt("Mood", Math.max(0, Math.min(100, Math.round(row.getFloat("Morale")))));
                    row.putInt("Satiety", Math.max(0, Math.min(100, (int) Math.floor(row.getFloat("Hunger")))));
                    row.putBoolean("Craving", history.craving());
                    history.requestedDish().ifPresent(value -> row.putString("RequestedDish", value.toString()));
                    history.repeatedDish().ifPresent(value -> row.putString("RepeatedDish", value.toString()));
                }
                defense.add(row);
            }
        }
        view.put("Defense", defense); view.putLong("ProjectedPayroll", projected); view.putLong("NextPayroll", data.workforce(town.id()).stream().filter(com.siirio.jemsettlements.workforce.WorkforceRecord::settlementActive).mapToLong(com.siirio.jemsettlements.workforce.WorkforceRecord::nextWageGameTime).filter(value -> value > 0L).min().orElse(0L)); view.putInt("MissedPayrolls", town.consecutiveMissedPayrolls());
        long unpaid = town.payroll().stream().filter(period -> period.currentCurrency() && !period.paid()).mapToLong(com.siirio.jemsettlements.payroll.PayrollPeriod::total).sum(); view.putLong("UnpaidPayroll", unpaid); view.putLong("PayrollShortfall", Math.max(0L, unpaid - com.siirio.jemsettlements.currency.SettlementCurrency.townBalance(player.getServer(), town.id())));
        Map<String, Long> stock = new LinkedHashMap<>(); int loaded = 0; long food = 0; int inventoryBudget = 8192;
        for (var link : town.storage()) {
            ServerLevel level = player.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, link.dimension()));
            if (level == null || !level.hasChunkAt(link.position())) continue;
            var optional = com.siirio.jemsettlements.runtime.SharedStorageService.handler(level, link); if (optional.isEmpty()) continue;
            loaded++;
            var handler = optional.get();
            for (int slot = 0; slot < handler.getSlots() && inventoryBudget-- > 0; slot++) {
                ItemStack stack = handler.getStackInSlot(slot); if (stack.isEmpty()) continue;
                String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); stock.merge(key, (long) stack.getCount(), Long::sum);
                if (stack.getItem().getFoodProperties(stack, null) != null) food += stack.getCount();
            }
        }
        view.putInt("LoadedStorage", loaded); view.putLong("FoodUnits", food);
        view.putLong("PendingNotSpendable", town.simulation().production().stream().filter(value -> !value.delivered()).count());
        view.putLong("FailedQuarantined", town.simulation().production().stream().filter(value -> value.status() == com.siirio.jemsettlements.simulation.ProductionObligation.Status.QUARANTINED).count());
        view.putBoolean("SimulationActive", town.simulation().active());
        ListTag journal = new ListTag(); for (var entry : town.simulation().journal()) { CompoundTag row = entry.save(); journal.add(row); } view.put("SimulationJournal", journal);
        ListTag production = new ListTag(); town.simulation().production().stream().skip(Math.max(0, town.simulation().production().size() - 16)).forEach(value -> production.add(value.save())); view.put("Production", production);
        ListTag items = new ListTag(); stock.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(32).forEach(entry -> {
            CompoundTag row = new CompoundTag(); row.putString("Item", entry.getKey()); row.putLong("Count", entry.getValue()); items.add(row);
        }); view.put("Stock", items);
        SettlementViewDetails.enrich(player, town, view);
        return view;
    }

    static void send(ServerPlayer player, TownViewPacket packet) {
        if (SettlementAuthority.active(player) && CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    private static void send(ServerPlayer player, FoodPolicyPacket packet) {
        if (SettlementAuthority.active(player) && CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
