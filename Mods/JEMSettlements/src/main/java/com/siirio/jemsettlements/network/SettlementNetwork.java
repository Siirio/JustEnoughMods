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
    private static final String VERSION = "12";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JemSettlements.MOD_ID, "town"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(value -> VERSION.equals(value) || NetworkRegistry.ABSENT.equals(value) || NetworkRegistry.ACCEPTVANILLA.equals(value)).serverAcceptedVersions(value -> VERSION.equals(value) || NetworkRegistry.ABSENT.equals(value) || NetworkRegistry.ACCEPTVANILLA.equals(value)).simpleChannel();
    private SettlementNetwork() {}

    static SimpleChannel channel() { return CHANNEL; }
    public static void sendBedSpaceNotice(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        for (ServerPlayer player : level.players()) if (player.distanceToSqr(entity) <= 24 * 24 && CHANNEL.isRemotePresent(player.connection.connection))
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BedSpaceNoticePacket(entity.getId(), entity.getUUID()));
    }
    public static boolean supported() { return ClientConnection.remote(CHANNEL); }
    public static void requestOpen() { action("open", null, new CompoundTag(), java.util.UUID.randomUUID()); }
    public static void requestFamilyHome(int entityId) { if (supported()) CHANNEL.sendToServer(new FamilyRequestPacket(entityId)); }
    public static void sendFamilyView(ServerPlayer player,int entityId,boolean open,CompoundTag view){if(CHANNEL.isRemotePresent(player.connection.connection))CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new FamilyViewPacket(entityId,open,view));}
    public static void sendFamilyComplaint(net.minecraft.world.entity.LivingEntity entity,String reason){if(!(entity.level() instanceof ServerLevel level))return;for(ServerPlayer player:level.players())if(player.distanceToSqr(entity)<=24*24&&CHANNEL.isRemotePresent(player.connection.connection))CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new FamilyComplaintPacket(entity.getId(),entity.getUUID(),reason));}
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
        CHANNEL.messageBuilder(FamilyComplaintPacket.class,11,NetworkDirection.PLAY_TO_CLIENT).encoder(FamilyComplaintPacket::encode).decoder(FamilyComplaintPacket::decode).consumerMainThread((packet,context)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemsettlements.client.FamilyComplaints.accept(packet));context.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(FamilyRequestPacket.class,10,NetworkDirection.PLAY_TO_SERVER).encoder(FamilyRequestPacket::encode).decoder(FamilyRequestPacket::decode).consumerMainThread((packet,context)->{var player=context.get().getSender();if(player!=null)com.siirio.jemsettlements.family.FamilyRuntime.request(player,packet.entityId());context.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(FamilyViewPacket.class,9,NetworkDirection.PLAY_TO_CLIENT).encoder(FamilyViewPacket::encode).decoder(FamilyViewPacket::decode).consumerMainThread((packet,context)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemsettlements.client.FamilyClientState.accept(packet));context.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(BedSpaceNoticePacket.class, 8, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BedSpaceNoticePacket::encode).decoder(BedSpaceNoticePacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemsettlements.client.BedSpaceNotices.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(StorageLabelsPacket.class,7,NetworkDirection.PLAY_TO_CLIENT).encoder(StorageLabelsPacket::encode).decoder(StorageLabelsPacket::decode).consumerMainThread((packet,context)->{ DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemsettlements.client.StorageLabels.accept(packet.view()));context.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(SettlementActionPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SettlementActionPacket::encode).decoder(SettlementActionPacket::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) SettlementActions.handle(player, packet);
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
        view.putBoolean("SetTaxAvailable",false);view.putInt("Happiness", town.happiness()); view.putInt("Tax", town.taxPercent()); view.putLong("Center", town.center().asLong());
        view.putInt("TerritoryRadius", town.territoryRadius()); view.putInt("TerritoryMinX", town.territoryMinX()); view.putInt("TerritoryMaxX", town.territoryMaxX()); view.putInt("TerritoryMinZ", town.territoryMinZ()); view.putInt("TerritoryMaxZ", town.territoryMaxZ()); view.putLong("TerritoryArea", town.territoryArea());
        view.putString("Tier", town.tier().name()); view.putString("PrimarySpecialization", town.primarySpecialization().name()); view.putString("SecondarySpecialization", town.secondarySpecialization().name());
        view.put("Metrics", town.metrics());
        view.putLong("LastResidentTax",town.metrics().getLong("LastResidentIncome"));view.putLong("LastBeautyTax",town.metrics().getLong("LastComfortIncome"));view.putLong("LastDailyTax",town.metrics().getLong("LastDailyTax"));
        view.putLong("CalculatedDailyIncome", town.dailyTax().getLong("CalculatedDailyIncome"));
        view.putInt("Housing", town.housingScore()); view.putInt("Food", town.foodScore()); view.putInt("Safety", town.safetyScore()); view.putInt("Jobs", town.jobsScore()); view.putInt("Community", town.communityScore()); view.putInt("Decoration", town.decorationScore());
        view.putLong("Treasury", SettlementApi.treasuryBalance(player, town));
        view.merge(com.siirio.jemsettlements.currency.SettlementCurrency.bulletin(player));
        view.putString("Dimension", town.dimension().toString()); view.putInt("StorageLinks", town.storage().size());
        var data = SettlementSavedData.get(player.getServer());
        view.put("Workforce", new CompoundTag());
        ListTag residents = new ListTag();
        for (var resident : data.residents(town.id()).stream().filter(com.siirio.jemsettlements.resident.ResidentMood::living).toList()) {
            CompoundTag row = new CompoundTag(); row.putUUID("Id", resident.villagerId()); row.putInt("Mood", resident.mood()); row.putInt("Satiety", resident.satiety()); row.putBoolean("Craving", resident.craving()); row.putBoolean("Living", resident.living());
            row.putInt("PersonalMood", resident.personalMood()); row.putInt("EnvironmentBonus", resident.environmentBonus()); row.putInt("SocialMoodCap", resident.socialMoodCap());
            row.putBoolean("CrowdedHome", resident.crowdedHome().isPresent());
            SettlementViewDetails.conditions(row, resident);
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
        view.put("WorkforceRows", new ListTag());
        ListTag defense = new ListTag(); long projected = 0L;
        var golems=town.metrics().getCompound("GolemObservations");
        for(String key:golems.getAllKeys()) {
            var row=golems.getCompound(key).copy();row.putString("Type","GOLEM");row.putString("Kind","GOLEM");
            row.putString("NativeType","minecraft:iron_golem");row.putBoolean("Living",true);row.putBoolean("Ready",row.getBoolean("Qualified"));
            if(!row.contains("Name"))row.putString("Name",net.minecraft.network.chat.Component.translatable("entity.minecraft.iron_golem").getString());
            defense.add(row);
        }
        view.put("Defense", defense);
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
