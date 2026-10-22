package com.siirio.jemserver.smp.shops;

import com.siirio.jemserver.smp.SmpQuery;
import com.siirio.jemserver.smp.ShopState;
import com.siirio.jemserver.smp.Navigation;
import com.siirio.jemserver.smp.Profiles;
import com.siirio.jemserver.smp.SmpConfig;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpNetwork;
import com.siirio.jemserver.smp.SmpRecords;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = "jem_server")
public final class ShopIndex {
    static record Position(ResourceLocation dimension, BlockPos block) {
        UUID id() {
            return UUID.nameUUIDFromBytes(
                    (dimension + ":" + block.asLong()).getBytes(StandardCharsets.UTF_8));
        }
    }

    static record ChunkPosition(ResourceLocation dimension, int x, int z) {
        static ChunkPosition of(Position position) {
            return new ChunkPosition(
                    position.dimension(),
                    position.block().getX() >> 4,
                    position.block().getZ() >> 4);
        }
    }

    static final class PositionIndex {
        private final Map<ChunkPosition, LinkedHashSet<Position>> positions = new HashMap<>();

        void add(Position position) {
            positions.computeIfAbsent(ChunkPosition.of(position), key -> new LinkedHashSet<>())
                    .add(position);
        }

        void remove(Position position) {
            var key = ChunkPosition.of(position);
            var bucket = positions.get(key);
            if (bucket == null) return;
            bucket.remove(position);
            if (bucket.isEmpty()) positions.remove(key);
        }

        Set<Position> at(ChunkPosition position) {
            var bucket = positions.get(position);
            return bucket == null ? Set.of() : Set.copyOf(bucket);
        }
    }

    private static final Map<MinecraftServer, LinkedHashSet<Position>> DIRTY = new WeakHashMap<>();
    private static final Map<MinecraftServer, PositionIndex> POSITIONS = new WeakHashMap<>();

    private static PositionIndex positions(MinecraftServer server) {
        return POSITIONS.computeIfAbsent(
                server,
                key -> {
                    var index = new PositionIndex();
                    for (var row : SmpData.get(server).shops().snapshots()) {
                        var dimension = ResourceLocation.tryParse(row.dimension());
                        if (dimension != null && row.hasPosition())
                            index.add(new Position(dimension, BlockPos.of(row.position())));
                    }
                    return index;
                });
    }

    public static void changed(BlockEntity block) {
        if (!(block.getLevel() instanceof ServerLevel level)) return;
        var position = new Position(level.dimension().location(), block.getBlockPos().immutable());
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> enqueue(level.getServer(), position));
            return;
        }
        enqueue(level.getServer(), position);
    }

    private static void enqueue(MinecraftServer server, Position position) {
        DIRTY.computeIfAbsent(server, key -> new LinkedHashSet<>()).add(position);
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        var server = level.getServer();
        var key =
                new ChunkPosition(
                        level.dimension().location(), chunk.getPos().x, chunk.getPos().z);
        server.execute(
                () -> {
                    positions(server).at(key).forEach(position -> enqueue(server, position));
                    chunk.getBlockEntities().values().stream()
                            .filter(block -> block instanceof ShopSnapshot)
                            .forEach(ShopIndex::changed);
                });
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var pending = DIRTY.get(event.getServer());
        if (pending == null) return;
        int remaining = SmpConfig.SHOP_BATCH.get();
        while (!pending.isEmpty() && remaining-- > 0) {
            var iterator = pending.iterator();
            var position = iterator.next();
            iterator.remove();
            refresh(event.getServer(), position);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        DIRTY.remove(event.getServer());
        POSITIONS.remove(event.getServer());
    }

    private static void refresh(MinecraftServer server, Position position) {
        SmpData.get(server).edit(() -> {
        var data = SmpData.get(server);
        var previous = data.shops().current( position.id());
        if (previous == null && data.shops().currentRows().size() >= SmpConfig.SHOP_LIMIT.get()) return;
        ServerLevel level =
                server.getLevel(ResourceKey.create(Registries.DIMENSION, position.dimension()));
        if (level == null || !level.hasChunkAt(position.block())) return;
        var entity = level.getBlockEntity(position.block());
        if (!(entity instanceof ShopSnapshot shop) || entity.isRemoved()) {
            remove(server, position);
            return;
        }
        var captured = shop.jemShopSnapshot();
        if (!captured.hasUUID("owner") || ItemStack.of(captured.getCompound("offer")).isEmpty()) {
            if (previous != null) remove(server, position);
            return;
        }
        captured.putUUID("id", position.id());
        captured.putString("dimension", position.dimension().toString());
        captured.putLong("position", position.block().asLong());
        captured.putInt("revision", previous == null ? 0 : previous.revision());
        captured.putLong("created", previous == null ? System.currentTimeMillis() : previous.created());
        ShopState row = ShopState.read(captured);
        if (!row.equals(previous)) {
            if (previous != null) row.revision(previous.revision() + 1);
            data.shops().put(row);
        }
        positions(server).add(position);
            });
    }

    public static boolean remove(
            MinecraftServer server, ResourceLocation dimension, BlockPos block) {
        return remove(server, new Position(dimension, block.immutable()));
    }

    private static boolean remove(MinecraftServer server, Position position) {
        return SmpData.get(server).edit(() -> {
        var data = SmpData.get(server);
        var removed = data.shops().remove( position.id());
        DIRTY.computeIfAbsent(server, key -> new LinkedHashSet<>()).remove(position);
        positions(server).remove(position);
        if (removed == null) return false;
        for (var profile : data.profiles().currentRows()) {
            profile.favorite(position.id(), false);
            
        }
        SmpNetwork.shopRemoved(server, position.id());
        return true;
            });
    }

    private static Position position(ShopState row) {
        var dimension = ResourceLocation.tryParse(row.dimension());
        return dimension == null || !row.hasPosition()
                ? null
                : new Position(dimension, BlockPos.of(row.position()));
    }

    private static void validateLoaded(MinecraftServer server, ShopState row) {
        var position = position(row);
        if (position == null) return;
        var level =
                server.getLevel(ResourceKey.create(Registries.DIMENSION, position.dimension()));
        if (level != null && level.hasChunkAt(position.block())) refresh(server, position);
    }

    /** Refresh native loaded shops explicitly before constructing a subscribed view. */
    public static void refreshLoaded(ServerPlayer player) {
        if (!com.siirio.jemserver.smp.DomainFeatures.economy(player.server)) return;
        var loaded =
                SmpData.get(player.server).shops().snapshots().stream()
                        .map(ShopIndex::position)
                        .filter(Objects::nonNull)
                        .filter(
                                position -> {
                                    var level =
                                            player.server.getLevel(
                                                    ResourceKey.create(
                                                            Registries.DIMENSION,
                                                            position.dimension()));
                                    return level != null && level.hasChunkAt(position.block());
                                })
                        .toList();
        loaded.forEach(position -> refresh(player.server, position));
    }

    public static CompoundTag view(ServerPlayer player, SmpQuery query) {
        if (!com.siirio.jemserver.smp.DomainFeatures.economy(player.server)) {
            CompoundTag result = new CompoundTag(); result.putString("tab", "shops");
            result.put("rows", new ListTag()); result.putInt("total", 0); result.putString("message", "unavailable");
            result.putString("moneyAvailability", com.siirio.jemserver.smp.DomainFeatures.economyAvailability(player.server)); return result;
        }
        var result = new CompoundTag();
        result.putString("tab", "shops");
        result.putString("filter", query.filter());
        result.putInt("page", query.page());
        result.putUUID("viewer", player.getUUID());
        result.putInt("pageSize", SmpConfig.PAGE_SIZE.get());
        String[] options = query.filter().split("\\|", 2);
        String section = options[0], sort = options.length > 1 ? options[1] : options[0];
        var favorites = Profiles.snapshot(player.server, player.getUUID()).favorites();
        String search = query.search().toLowerCase(Locale.ROOT);
        var matches =
                SmpData.get(player.server).shops().snapshots().stream()
                        .filter(
                                row ->
                                        switch (section) {
                                            case "favorites" ->
                                                    favorites.contains(row.id());
                                            case "mine" ->
                                                    row.owner() != null
                                                            && row.owner()
                                                                    .equals(player.getUUID());
                                            default -> true;
                                        })
                        .filter(row -> matches(row, search))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Comparator<ShopState> order = Comparator.comparing(row -> row.id().toString());
        if (sort.equals("near"))
            order =
                    Comparator.comparingDouble((ShopState row) -> distance(player, row))
                            .thenComparing(order);
        if (sort.equals("price") && !matches.isEmpty()) {
            var payment = ItemStack.of(matches.get(0).payment().tag());
            var offer = ItemStack.of(matches.get(0).offer().tag());
            boolean comparable =
                    !payment.isEmpty()
                            && !offer.isEmpty()
                            && matches.stream()
                                    .allMatch(
                                            row ->
                                                    !row.selectable()
                                                            && row.quantity() > 0
                                                            && ItemStack.isSameItemSameTags(
                                                                    payment,
                                                                    ItemStack.of(
                                                                            row.payment().tag()))
                                                            && ItemStack.isSameItemSameTags(
                                                                    offer,
                                                                    ItemStack.of(
                                                                            row.offer().tag())));
            result.putBoolean("priceComparable", comparable);
            if (comparable)
                order =
                        Comparator.comparingDouble(
                                        (ShopState row) ->
                                                (double) row.price()
                                                        / row.quantity())
                                .thenComparing(order);
            else result.putString("message", "incomparable_prices");
        }
        matches.sort(order);
        result.putInt("total", matches.size());
        var rows = new ListTag();
        matches.stream()
                .skip((long) query.page() * SmpConfig.PAGE_SIZE.get())
                .limit(SmpConfig.PAGE_SIZE.get())
                .forEach(row -> rows.add(publicView(player, row)));
        result.put("rows", rows);
        if (query.selected() != null) {
            var selected = SmpData.get(player.server).shops().snapshot( query.selected());
            if (selected == null)
                selected =
                        matches.stream()
                                .filter(row -> row.id().equals(query.selected()))
                                .findFirst()
                                .orElse(null);
            if (selected != null) {
                var detail = publicView(player, selected);
                var actions = new ListTag();
                actions.add(StringTag.valueOf("favorite"));
                actions.add(StringTag.valueOf("navigate"));
                detail.put("actions", actions);
                detail.putBoolean("favorite", favorites.contains(query.selected()));
                result.put("detail", detail);
            }
        }
        return result;
    }

    private static boolean matches(ShopState row, String search) {
        String seller = "", wanted = search.trim();
        if (search.contains("\u001f")) {
            String[] parts = search.split("\u001f", 2);
            seller = parts[0].trim();
            wanted = parts[1].trim();
        } else if (search.startsWith("player:")) {
            seller = search.substring("player:".length()).trim();
            wanted = "";
        } else if (search.startsWith("item:")) wanted = search.substring("item:".length()).trim();
        if (!row.name().toLowerCase(Locale.ROOT).contains(seller)) return false;
        var item = ItemStack.of(row.offer().tag());
        String offered = row.title() + " " + item.getHoverName().getString();
        for (ShopState.Stack value : row.choices()) {
            var choice = ItemStack.of(value.tag());
            offered += " " + choice.getHoverName().getString();
        }
        return offered.toLowerCase(Locale.ROOT).contains(wanted);
    }

    private static CompoundTag publicView(ServerPlayer player, ShopState row) {
        CompoundTag encoded = row.write();
        var result = new CompoundTag();
        for (String key :
                List.of(
                        "id",
                        "owner",
                        "title",
                        "name",
                        "state",
                        "revision",
                        "dimension",
                        "position",
                        "quantity",
                        "price",
                        "selectable")) {
            if (encoded.contains(key)) result.put(key, encoded.get(key).copy());
        }
        for (String key : List.of("title", "name")) {
            String text = result.getString(key);
            if (text.length() > SmpConfig.MAX_TEXT.get()) {
                result.putString(key, text.substring(0, SmpConfig.MAX_TEXT.get()));
                result.putBoolean("itemDetailsTruncated", true);
            }
        }
        int remaining = SmpConfig.SHOP_STACK_NBT_BYTES.get();
        for (String key : List.of("offer", "payment")) {
            var stack = boundedStack(encoded.getCompound(key), remaining);
            remaining -= stack.sizeInBytes();
            result.put(key, stack);
            if (!stack.equals(encoded.getCompound(key)))
                result.putBoolean("itemDetailsTruncated", true);
        }
        var choices = new ListTag();
        for (Tag value : encoded.getList("choices", Tag.TAG_COMPOUND)) {
            var stack = boundedStack((CompoundTag) value, remaining);
            remaining -= stack.sizeInBytes();
            choices.add(stack);
            if (!stack.equals(value)) result.putBoolean("itemDetailsTruncated", true);
        }
        if (!choices.isEmpty()) result.put("choices", choices);
        var offers = new ListTag();
        for (Tag value : encoded.getList("offers", Tag.TAG_COMPOUND)) {
            var source = (CompoundTag) value;
            var offer = new CompoundTag();
            var payment = boundedStack(source.getCompound("payment"), remaining);
            remaining -= payment.sizeInBytes();
            var output = boundedStack(source.getCompound("output"), remaining);
            remaining -= output.sizeInBytes();
            offer.put("payment", payment);
            offer.put("output", output);
            offer.putInt("availableTrades", source.getInt("availableTrades"));
            offers.add(offer);
            if (!payment.equals(source.getCompound("payment")) || !output.equals(source.getCompound("output"))) {
                result.putBoolean("itemDetailsTruncated", true);
            }
        }
        if (!offers.isEmpty()) result.put("offers", offers);
        if (encoded.hasUUID("owner")) result.putBoolean("ownerOnline", player.server.getPlayerList().getPlayer(encoded.getUUID("owner")) != null);
        if (!encoded.contains("dimension")) return result;
        var level =
                player.server.getLevel(
                        ResourceKey.create(
                                Registries.DIMENSION,
                                new ResourceLocation(encoded.getString("dimension"))));
        var block = BlockPos.of(encoded.getLong("position"));
        boolean loaded = level != null && level.hasChunkAt(block);
        result.putBoolean("remoteAvailable", loaded && level.getBlockEntity(block) instanceof ShopSnapshot);
        double distance = distance(player, row);
        if (Double.isFinite(distance)) result.putDouble("distance", Math.sqrt(distance));
        return result;
    }

    private static CompoundTag boundedStack(CompoundTag stack, int remaining) {
        if (stack.sizeInBytes() <= remaining) return stack.copy();
        var compact = new CompoundTag();
        compact.putString("id", stack.getString("id"));
        compact.putByte("Count", stack.getByte("Count"));
        return compact;
    }

    private static double distance(ServerPlayer player, ShopState row) {
        return player.level().dimension().location().toString().equals(row.dimension())
                ? player.blockPosition().distSqr(BlockPos.of(row.position()))
                : Double.POSITIVE_INFINITY;
    }

    public static void favorite(ServerPlayer player, UUID id) {
        var data = SmpData.get(player.server);
        data.edit(() -> favoriteOwned(player, id));
    }

    private static void favoriteOwned(ServerPlayer player, UUID id) {
        var row = id == null ? null : SmpData.get(player.server).shops().snapshot( id);
        if (id == null) throw new com.siirio.jemserver.smp.SmpActionFailure("unavailable");
        var profile = Profiles.get(player.server, player.getUUID());
        var favorites = profile.favorites();
        if (favorites.contains(id)) profile.favorite(id, false);
        else {
            if (row == null) throw new com.siirio.jemserver.smp.SmpActionFailure("unavailable");
            if (favorites.size() >= SmpConfig.SHOP_FAVORITES_LIMIT.get())
                throw new com.siirio.jemserver.smp.SmpActionFailure("favorite_limit");
            profile.favorite(id, true);
        }
    }

    public static void buy(ServerPlayer player, UUID id, int expectedRevision, UUID requestId) {
        var data = SmpData.get(player.server);
        data.edit(() -> buyOwned(player, id, expectedRevision, requestId));
    }

    private static void buyOwned(ServerPlayer player, UUID id, int expectedRevision, UUID requestId) {
        var row = id == null ? null : SmpData.get(player.server).shops().snapshot(id);
        if (row == null) throw new com.siirio.jemserver.smp.SmpActionFailure("unavailable");
        validateLoaded(player.server, row);
        row = SmpData.get(player.server).shops().snapshot(id);
        if (row == null) throw new com.siirio.jemserver.smp.SmpActionFailure("unavailable");
        if (!net.minecraftforge.fml.ModList.get().isLoaded("spudaciousshops")) throw new com.siirio.jemserver.smp.SmpActionFailure("unavailable");
        buyWithCoins(player, row, expectedRevision, requestId);
    }

    public static void navigate(ServerPlayer player, UUID id) {
        var data = SmpData.get(player.server);
        data.edit(() -> navigateOwned(player, id));
    }

    private static void navigateOwned(ServerPlayer player, UUID id) {
        var row = id == null ? null : SmpData.get(player.server).shops().snapshot(id);
        if (row == null)
            throw new com.siirio.jemserver.smp.SmpActionFailure("unavailable");
        Navigation.send(
                player,
                "shops",
                id,
                row.title(),
                new ResourceLocation(row.dimension()),
                BlockPos.of(row.position()),
                System.currentTimeMillis()
                        + java.util.concurrent.TimeUnit.MINUTES.toMillis(
                                SmpConfig.WAYPOINT_MINUTES.get()));
    }

    private static void buyWithCoins(ServerPlayer player, ShopState row, int expectedRevision, UUID requestId) {
        SmpRecords.require(com.siirio.jemserver.smp.DomainFeatures.economy(player.server), "unavailable");
        SmpRecords.require(expectedRevision == row.revision(), "stale_revision");
        SmpRecords.require(!row.selectable(), "select_offer_at_shop");
        SmpRecords.require(row.owner() != null && !row.owner().equals(player.getUUID()), "invalid_request");
        SmpRecords.require(row.state().equals("ACTIVE"), "unavailable");
        int price = row.price();
        int quantity = row.quantity();
        ItemStack expected = ItemStack.of(row.offer().tag());
        SmpRecords.require(price > 0 && price <= 1_000_000_000 && quantity > 0 && quantity <= 64 && !expected.isEmpty(), "invalid_request");
        expected.setCount(quantity);
        SmpRecords.require(canAccept(player, expected), "inventory_full");

        var position = position(row);
        SmpRecords.require(position != null, "unavailable");
        ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, position.dimension()));
        SmpRecords.require(level != null && level.hasChunkAt(position.block()), "shop_unloaded");
        var block = level.getBlockEntity(position.block());
        SmpRecords.require(block instanceof ShopCoinAccess, "unavailable");
        var shop = (ShopCoinAccess) block;

        var reserve = com.siirio.jemserver.compat.economy.CoinPayments.reserve(player, price, requestId);
        if (reserve == com.siirio.jemserver.compat.economy.CoinPayments.Status.REPLAYED) return;
        SmpRecords.require(reserve == com.siirio.jemserver.compat.economy.CoinPayments.Status.RESERVED,
                reserve == com.siirio.jemserver.compat.economy.CoinPayments.Status.INSUFFICIENT_FUNDS ? "insufficient_coins" : "unavailable");

        ItemStack sold = shop.jemTakeCoinSale(expected, quantity);
        if (sold.isEmpty()) {
            com.siirio.jemserver.compat.economy.CoinPayments.refund(player, price, requestId);
            throw new com.siirio.jemserver.smp.SmpActionFailure("out_of_stock");
        }
        if (!player.getInventory().add(sold) || !sold.isEmpty()) {
            shop.jemRestoreCoinSale(sold);
            com.siirio.jemserver.compat.economy.CoinPayments.refund(player, price, requestId);
            throw new com.siirio.jemserver.smp.SmpActionFailure("inventory_full");
        }
        var settled = com.siirio.jemserver.compat.economy.CoinPayments.settle(player, row.owner(), price, requestId);
        if (settled != com.siirio.jemserver.compat.economy.CoinPayments.Status.COMMITTED
                && settled != com.siirio.jemserver.compat.economy.CoinPayments.Status.REPLAYED) {
            throw new com.siirio.jemserver.smp.SmpActionFailure("settlement_pending");
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        refresh(player.server, position);
    }

    private static boolean canAccept(ServerPlayer player, ItemStack offered) {
        int remaining = offered.getCount();
        for (ItemStack current : player.getInventory().items) {
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(current, offered)) {
                remaining -= Math.max(0, Math.min(current.getMaxStackSize(), player.getInventory().getMaxStackSize()) - current.getCount());
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    private ShopIndex() {}
}
