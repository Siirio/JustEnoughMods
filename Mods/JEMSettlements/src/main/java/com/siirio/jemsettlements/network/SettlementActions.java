package com.siirio.jemsettlements.network;

import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Owns authorization, selected UUID and request acknowledgments for every standalone screen. */
public final class SettlementActions {
    private static final Map<UUID, Subscription> VIEWERS = new HashMap<>();
    private static int viewerCursor;
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    private static final java.util.Set<String> READS = java.util.Set.of("open", "select", "refresh", "close", "guide", "heartbeat", "mapopen", "housing", "city", "storage");
    private SettlementActions() { }

    public static void handle(ServerPlayer player, SettlementActionPacket packet) {
        if (!SettlementAuthority.active(player)) { acknowledge(player, packet, SettlementApi.Result.UNAUTHENTICATED, packet.townId()); return; }
        Subscription existingSession = VIEWERS.get(player.getUUID());
        if (!packet.action().equals("open") && !packet.action().equals("guide") && !packet.action().equals("mapopen") &&
                (existingSession == null || !packet.payload().hasUUID("Session") || !existingSession.session.equals(packet.payload().getUUID("Session")))) {
            transientReply(player, packet, "RETRY"); return;
        }
        long now = player.getServer().overworld().getGameTime();
        long previous = LAST_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (previous != Long.MIN_VALUE && now - previous < 2L) { transientReply(player, packet, "RETRY"); return; }
        LAST_REQUEST.put(player.getUUID(), now);
        if (READS.contains(packet.action())) {
            if (packet.action().equals("close")) { VIEWERS.remove(player.getUUID()); return; }
            if (packet.action().equals("heartbeat")) {
                var heartbeat = VIEWERS.get(player.getUUID()); heartbeat.expires = now + 240L;
                CompoundTag response = new CompoundTag(); response.putUUID("Session", heartbeat.session); response.putBoolean("Heartbeat", true);
                SettlementNetwork.send(player, new TownViewPacket(false, response)); return;
            }
            if (packet.action().equals("guide")) { SettlementNetwork.sendGuide(player); return; }
            Subscription subscription = VIEWERS.computeIfAbsent(player.getUUID(), id -> new Subscription());
            subscription.offset = Math.max(0, packet.payload().getInt("Offset")); subscription.foodOffset = Math.max(0, packet.payload().getInt("FoodOffset"));
            subscription.expires = now + 240L;
            if (packet.action().equals("storage")) {
                if (SettlementApi.ownedTown(player, packet.townId()).isEmpty() || !packet.payload().contains("Position")) {
                    acknowledge(player, packet, SettlementApi.Result.UNAUTHENTICATED, packet.townId()); return;
                }
                BlockPos requestedStorage = BlockPos.of(packet.payload().getLong("Position"));
                if (SettlementApi.ownedTown(player, packet.townId()).orElseThrow().storage().stream().noneMatch(link -> link.contains(requestedStorage))) {
                    acknowledge(player, packet, SettlementApi.Result.INVALID, packet.townId()); return;
                }
                subscription.storage = requestedStorage;
                subscription.slotOffset = Math.max(0, packet.payload().getInt("SlotOffset"));
            } else if (packet.action().equals("select") || packet.action().equals("open")) subscription.storage = null;
            if (packet.action().equals("mapopen")) { subscription.map = true; }
            else if (packet.action().equals("open")) { subscription.map = false; subscription.town = null; open(player, false); }
            else if (packet.action().equals("select")) { subscription.map = false; select(player, packet.townId(), false); }
            else if (!subscription.map) select(player, packet.townId(), false);
            CompoundTag response = envelope(player, subscription); response.putUUID("Request", packet.requestId()); response.putString("Result", "ACCEPTED");
            if(packet.action().equals("housing")) {
                var owned=SettlementApi.ownedTown(player,packet.townId());
                if(owned.isPresent() && packet.payload().hasUUID("Entity")) SettlementSavedData.get(player.getServer()).resident(packet.payload().getUUID("Entity"))
                    .filter(resident->resident.townId().equals(owned.get().id())&&resident.living()).ifPresent(resident->response.put("Housing",com.siirio.jemsettlements.happiness.HousingInspection.capture(player.getServer(),resident,player)));
            }
            if(packet.action().equals("city")) {
                SettlementApi.ownedTown(player,packet.townId()).ifPresent(owned -> response.put("City",com.siirio.jemsettlements.happiness.CityInspection.capture(player,owned)));
            }
            SettlementNetwork.send(player, new TownViewPacket(false, response));
            return;
        }
        UUID actor = SettlementAuthority.principal(player).orElse(null);
        if (actor == null) { acknowledge(player, packet, SettlementApi.Result.UNAUTHENTICATED, packet.townId()); return; }
        SettlementRequests journal = SettlementRequests.get(player.getServer());
        CompoundTag prior = journal.prior(actor, packet.requestId());
        if (prior != null && !prior.getString("Terms").equals(SettlementRequests.terms(packet))) {
            acknowledge(player, packet, SettlementApi.Result.CONFLICT, packet.townId()); return;
        }
        if (prior != null && prior.contains("Result")) {
            SettlementApi.Result result = SettlementApi.Result.valueOf(prior.getString("Result"));
            acknowledge(player, packet, result == SettlementApi.Result.ACCEPTED ? SettlementApi.Result.REPLAYED : result,
                    prior.hasUUID("Town") ? prior.getUUID("Town") : packet.townId()); return;
        }
        if (prior != null) {
            CompoundTag uncertain = envelope(player, VIEWERS.get(player.getUUID())); uncertain.putUUID("Request", packet.requestId());
            uncertain.putString("Result", "OUTCOME_UNKNOWN"); uncertain.putString("Reason", "jem.settlements.reason.outcome_unknown");
            SettlementNetwork.send(player, new TownViewPacket(false, uncertain)); return;
        }
        if (journal.uncertain(actor) >= 32) { transientReply(player, packet, "OUTCOME_UNKNOWN"); return; }
        if (packet.requestId().getMostSignificantBits() != existingSession.session.getMostSignificantBits()) {
            acknowledge(player, packet, SettlementApi.Result.UNAVAILABLE, packet.townId()); return;
        }
        var activeSessions = VIEWERS.values().stream().map(s -> s.session.getMostSignificantBits()).collect(java.util.stream.Collectors.toSet());
        if (!journal.admit(actor, packet, activeSessions)) { acknowledge(player, packet, SettlementApi.Result.UNAVAILABLE, packet.townId()); return; }
        player.getServer().overworld().getDataStorage().save();
        UUID affected = packet.townId();
        SettlementApi.Result result;
        try {
            if (packet.action().equals("create")) {
                BlockPos bell = BlockPos.of(packet.payload().getLong("Bell"));
                result = SettlementApi.create(player, packet.payload().getString("Name"), bell, packet.requestId());
                affected = UUID.nameUUIDFromBytes(("settlement:create:" + actor + ":" + packet.requestId()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (result == SettlementApi.Result.ACCEPTED) com.siirio.jemsettlements.runtime.SettlementRuntime.celebrateFounding(player.serverLevel(), bell);
            } else result = mutate(player, packet);
        } catch (IllegalArgumentException | ArithmeticException invalid) { result = SettlementApi.Result.INVALID; }
        player.getServer().getPlayerList().saveAll();
        player.getServer().overworld().getDataStorage().save();
        if (result == SettlementApi.Result.UNAVAILABLE && java.util.Set.of("create", "expand", "mapexpand", "deposit", "withdraw", "delete", "payroll").contains(packet.action())) {
            transientReply(player, packet, "OUTCOME_UNKNOWN"); return;
        }
        journal.finish(actor, packet.requestId(), result.name(), affected, now);
        player.getServer().overworld().getDataStorage().save();
        if (++existingSession.completed >= 512) { existingSession.session = UUID.randomUUID(); existingSession.completed = 0; }
        acknowledge(player, packet, result, affected);
    }

    private static SettlementApi.Result mutate(ServerPlayer player, SettlementActionPacket packet) {
        var town = SettlementApi.ownedTown(player, packet.townId());
        if (town.isEmpty()) return SettlementApi.Result.UNAUTHENTICATED;
        CompoundTag data = packet.payload(); UUID id = town.get().id();
        return switch (packet.action()) {
            case "tax" -> SettlementApi.setTax(player,id,data.getInt("Percent"));
            case "rename" -> SettlementApi.rename(player, id, data.getString("Name"));
            case "delete" -> SettlementApi.delete(player, id, packet.requestId());
            case "remove" -> data.hasUUID("Entity") ? SettlementApi.removeRegistration(player, id, data.getUUID("Entity")) : SettlementApi.Result.INVALID;
            case "deposit", "withdraw" -> SettlementApi.treasury(player, id, packet.action().equals("deposit"), data.getLong("Amount"), packet.requestId());
            case "expand", "mapexpand" -> expand(player, packet, town.get());
            case "foodstorage" -> SettlementApi.setFoodStorage(player, id, BlockPos.of(data.getLong("Position")), data.getBoolean("Enabled"));
            case "linkstorage" -> SettlementApi.linkPhysicalStorage(player, id, BlockPos.of(data.getLong("Position")));
            case "feedall" -> data.contains("Position") ? com.siirio.jemsettlements.runtime.TownMealService.request(player, town.get(), BlockPos.of(data.getLong("Position"))) : SettlementApi.Result.INVALID;
            case "manage" -> data.hasUUID("Entity") ? SettlementApi.openNativeManagement(player, id, data.getUUID("Entity")) : SettlementApi.Result.INVALID;
            case "buyback" -> data.hasUUID("Entity") && net.minecraftforge.fml.ModList.get().isLoaded("recruits")
                    ? SettlementApi.openRecruitBuyback(player, id, data.getUUID("Entity")) : SettlementApi.Result.UNAVAILABLE;
            case "payroll" -> {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("recruits")) yield SettlementApi.Result.UNAVAILABLE;
                com.siirio.jemsettlements.payroll.PayrollService.tick(player.getServer());
                yield town.get().payroll().stream().allMatch(p -> !p.currentCurrency() || p.paid()) ? SettlementApi.Result.ACCEPTED : SettlementApi.Result.INSUFFICIENT_FUNDS;
            }
            default -> SettlementApi.Result.INVALID;
        };
    }
    private static SettlementApi.Result expand(ServerPlayer player, SettlementActionPacket packet, TownRecord town) {
        CompoundTag p = packet.payload();
        if (!p.contains("ExpectedMinX") || !p.contains("ExpectedMaxX") || !p.contains("ExpectedMinZ") || !p.contains("ExpectedMaxZ")
                || p.getInt("ExpectedMinX") != town.territoryMinX() || p.getInt("ExpectedMaxX") != town.territoryMaxX()
                || p.getInt("ExpectedMinZ") != town.territoryMinZ() || p.getInt("ExpectedMaxZ") != town.territoryMaxZ()) return SettlementApi.Result.CONFLICT;
        int x = p.getInt("X"), z = p.getInt("Z");
        if (!packet.action().equals("mapexpand") && ((long) x < (long) town.territoryMinX() - 1L || (long) x > (long) town.territoryMaxX() + 1L
                || (long) z < (long) town.territoryMinZ() - 1L || (long) z > (long) town.territoryMaxZ() + 1L)) return SettlementApi.Result.INVALID;
        long expected = com.siirio.jemsettlements.town.TerritoryQuote.union(town.territoryMinX(), town.territoryMinZ(),
                town.territoryMaxX(), town.territoryMaxZ(), x, z, x, z).addedCopper();
        if (!p.contains("ExpectedCost") || p.getLong("ExpectedCost") != expected
                || p.getBoolean("ExpectedCoins") != com.siirio.jemsettlements.currency.SettlementCurrency.coins()) return SettlementApi.Result.CONFLICT;
        return SettlementApi.expandTerritory(player, town.id(), x, z, packet.requestId());
    }
    private static boolean successful(SettlementApi.Result result) { return result == SettlementApi.Result.ACCEPTED || result == SettlementApi.Result.REPLAYED; }

    private static void transientReply(ServerPlayer player, SettlementActionPacket packet, String result) {
        Subscription sub = VIEWERS.computeIfAbsent(player.getUUID(), ignored -> new Subscription());
        sub.expires = player.getServer().overworld().getGameTime() + 240L;
        CompoundTag view = envelope(player, sub); view.putUUID("Request", packet.requestId()); view.putString("Result", result);
        view.putString("Reason", "jem.settlements.reason." + result.toLowerCase(java.util.Locale.ROOT));
        SettlementNetwork.send(player, new TownViewPacket(false, view));
    }

    private static void acknowledge(ServerPlayer player, SettlementActionPacket packet, SettlementApi.Result result, UUID affected) {
        Subscription subscription = VIEWERS.computeIfAbsent(player.getUUID(), ignored -> new Subscription());
        if (successful(result)) subscription.town = packet.action().equals("delete") ? null : affected;
        subscription.expires = player.getServer().overworld().getGameTime() + 240L;
        CompoundTag view = envelope(player, subscription);
        view.putUUID("Request", packet.requestId()); view.putString("Result", result.name());
        view.putString("Reason", "jem.settlements.reason." + result.name().toLowerCase(java.util.Locale.ROOT));
        if (packet.action().equals("delete") && successful(result)) view.putBoolean("Deleted", true);
        view.putString("Action", packet.action());
        SettlementNetwork.send(player, new TownViewPacket(false, view));
    }
    static void open(ServerPlayer player, boolean open) {
        if (!SettlementAuthority.active(player)) return;
        Subscription sub = VIEWERS.computeIfAbsent(player.getUUID(), ignored -> new Subscription());
        sub.map = false;
        var towns = SettlementApi.ownedTowns(player);
        if (open || sub.town == null || SettlementApi.ownedTown(player, sub.town).isEmpty()) sub.town = towns.size() == 1 ? towns.get(0).id() : null;
        sub.expires = player.getServer().overworld().getGameTime() + 240L;
        SettlementNetwork.send(player, new TownViewPacket(open, envelope(player, sub)));
    }
    static void select(ServerPlayer player, UUID id, boolean open) {
        if (!SettlementAuthority.active(player)) return;
        Subscription sub = VIEWERS.computeIfAbsent(player.getUUID(), ignored -> new Subscription());
        sub.map = false;
        sub.town = SettlementApi.ownedTown(player, id).map(TownRecord::id).orElse(null);
        sub.expires = player.getServer().overworld().getGameTime() + 240L;
        SettlementNetwork.send(player, new TownViewPacket(open, envelope(player, sub)));
    }
    static void founding(ServerPlayer player, BlockPos bell) {
        if (!SettlementAuthority.active(player)) return;
        Subscription sub = VIEWERS.computeIfAbsent(player.getUUID(), ignored -> new Subscription());
        sub.map = false;
        sub.expires = player.getServer().overworld().getGameTime() + 240L;
        CompoundTag view = envelope(player, sub); view.putBoolean("Founding", true); view.putLong("Bell", bell.asLong());
        SettlementNetwork.send(player, new TownViewPacket(true, view));
    }
    private static CompoundTag envelope(ServerPlayer player, Subscription sub) {
        if (sub.map) return mapEnvelope(player, sub);
        CompoundTag view = new CompoundTag(); ListTag directory = new ListTag(); var towns = SettlementApi.ownedTowns(player);
        int offset = Math.min(sub.offset, Math.max(0, ((towns.size() - 1) / 64) * 64));
        for (TownRecord town : towns.stream().skip(offset).limit(64).toList()) {
            CompoundTag row = new CompoundTag(); row.putUUID("Id", town.id()); row.putString("Name", town.name()); row.putString("Dimension", town.dimension().toString());
            row.putInt("Happiness", town.happiness()); row.putInt("Residents", (int) SettlementSavedData.get(player.getServer()).residents(town.id()).stream().filter(r -> r.living()).count());
            row.putBoolean("Warning", town.happiness() < 35 || town.payroll().stream().anyMatch(p -> p.currentCurrency() && !p.paid())); directory.add(row);
        }
        view.putUUID("Session", sub.session); view.put("Directory", directory); view.putInt("DirectoryOffset", offset); view.putInt("DirectoryTotal", towns.size());
        if (sub.town != null) SettlementApi.ownedTown(player, sub.town).ifPresentOrElse(t -> {
            CompoundTag selected = SettlementNetwork.buildView(player, t); SettlementViewDetails.foodPage(player, t, selected, sub.foodOffset);
            if (sub.storage != null) selected.put("SelectedStorage", SettlementViewDetails.storagePage(player, t, sub.storage, sub.slotOffset));
            view.put("Selected", selected);
        }, () -> { sub.town = null; view.putBoolean("Deleted", true); });
        view.putLong("GameTime", player.getServer().overworld().getGameTime());
        com.siirio.jemsettlements.runtime.SettlementAuthority.principal(player).ifPresent(actor -> view.putInt("UncertainActions", SettlementRequests.get(player.getServer()).uncertain(actor)));
        view.putBoolean("WorkersAvailable", false);
        view.putBoolean("RecruitsAvailable", net.minecraftforge.fml.ModList.get().isLoaded("recruits"));
        view.putBoolean("CoinCurrency", com.siirio.jemsettlements.currency.SettlementCurrency.coins());
        view.putLong("FoundingCost", com.siirio.jemsettlements.payroll.SettlementEconomyConfig.founding());
        view.putString("FoundingDimension", player.serverLevel().dimension().location().toString());
        view.putInt("InitialRadius", TownRecord.INITIAL_RADIUS);
        var hit = player.pick(16.0D, 0.0F, false);
        if (hit instanceof net.minecraft.world.phys.BlockHitResult block && player.serverLevel().getBlockState(block.getBlockPos()).is(net.minecraft.world.level.block.Blocks.BELL)) view.putLong("Bell", block.getBlockPos().asLong());
        return view;
    }
    /** Same public territorial projection as the built-in map; no storage, residents or personal waypoints. */
    private static CompoundTag mapEnvelope(ServerPlayer player, Subscription sub) {
        CompoundTag view = new CompoundTag(); view.putUUID("Session", sub.session); view.putBoolean("MapOnly", true);
        view.putBoolean("CoinCurrency", com.siirio.jemsettlements.currency.SettlementCurrency.coins());
        view.putLong("ExpansionCostPerBlock", com.siirio.jemsettlements.payroll.SettlementEconomyConfig.expansionPerBlock());
        view.putLong("ExpansionMinimumCost", com.siirio.jemsettlements.payroll.SettlementEconomyConfig.expansionMinimum());
        ListTag areas = new ListTag(); UUID owner = SettlementAuthority.principal(player).orElse(null);
        for (TownRecord town : SettlementSavedData.get(player.getServer()).towns().stream().limit(256).toList()) {
            CompoundTag row = new CompoundTag(); row.putUUID("Id", town.id()); row.putString("Name", town.name());
            row.putString("Dimension", town.dimension().toString()); row.putLong("Center", town.center().asLong());
            row.putBoolean("Owned", town.owner().equals(owner));
            row.putInt("MinX", town.territoryMinX()); row.putInt("MaxX", town.territoryMaxX());
            row.putInt("MinZ", town.territoryMinZ()); row.putInt("MaxZ", town.territoryMaxZ()); areas.add(row);
        }
        view.put("MapAreas", areas); return view;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0 || VIEWERS.isEmpty()) return;
        long now = event.getServer().overworld().getGameTime();
        VIEWERS.entrySet().removeIf(entry -> event.getServer().getPlayerList().getPlayer(entry.getKey()) == null || now > entry.getValue().expires);
        var viewers = new java.util.ArrayList<>(VIEWERS.entrySet());
        if (viewers.isEmpty()) return;
        int count = Math.min(4, viewers.size());
        for (int i = 0; i < count; i++) {
            var entry = viewers.get(Math.floorMod(viewerCursor++, viewers.size()));
            var player = event.getServer().getPlayerList().getPlayer(entry.getKey()); var sub = entry.getValue();
            if (player == null || !SettlementAuthority.active(player)) continue;
            CompoundTag view = envelope(player, sub); CompoundTag stable = view.copy(); stable.remove("GameTime"); stable.remove("Bell");
            if (stable.contains("Selected")) stable.getCompound("Selected").remove("GameTime");
            if (!stable.equals(sub.last)) { sub.last = stable; SettlementNetwork.send(player, new TownViewPacket(false, view)); }
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { VIEWERS.remove(event.getEntity().getUUID()); LAST_REQUEST.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { VIEWERS.clear(); LAST_REQUEST.clear(); SettlementViewDetails.invalidate(); }
    @SubscribeEvent public static void datapacks(net.minecraftforge.event.OnDatapackSyncEvent event) {
        SettlementViewDetails.invalidate();
        if (event.getPlayer() != null) SettlementNetwork.sendFoodPolicy(event.getPlayer());
        else event.getPlayerList().getPlayers().forEach(SettlementNetwork::sendFoodPolicy);
    }
    private static final class Subscription { UUID session = UUID.randomUUID(); UUID town; int completed; int offset; int foodOffset; BlockPos storage; int slotOffset; boolean map; long expires; CompoundTag last; }
}
