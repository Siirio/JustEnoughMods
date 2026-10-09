package com.siirio.jemserver.smp.shops;

import com.siirio.jemserver.smp.Navigation;
import com.siirio.jemserver.smp.Profiles;
import com.siirio.jemserver.smp.SmpConfig;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpNetwork;

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

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
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
                    for (var row : SmpData.get(server).all("shops")) {
                        var dimension = ResourceLocation.tryParse(row.getString("dimension"));
                        if (dimension != null && row.contains("position", Tag.TAG_LONG))
                            index.add(new Position(dimension, BlockPos.of(row.getLong("position"))));
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
        var data = SmpData.get(server);
        var previous = data.find("shops", position.id());
        if (previous == null && data.all("shops").size() >= SmpConfig.SHOP_LIMIT.get()) return;
        ServerLevel level =
                server.getLevel(ResourceKey.create(Registries.DIMENSION, position.dimension()));
        if (level == null || !level.hasChunkAt(position.block())) return;
        var entity = level.getBlockEntity(position.block());
        if (!(entity instanceof ShopSnapshot shop) || entity.isRemoved()) {
            remove(server, position);
            return;
        }
        var row = shop.jemShopSnapshot();
        if (!row.hasUUID("owner") || ItemStack.of(row.getCompound("offer")).isEmpty()) {
            if (previous != null) remove(server, position);
            return;
        }
        row.putUUID("id", position.id());
        row.putString("dimension", position.dimension().toString());
        row.putLong("position", position.block().asLong());
        row.putInt("revision", previous == null ? 0 : previous.getInt("revision"));
        row.putLong(
                "created",
                previous == null ? System.currentTimeMillis() : previous.getLong("created"));
        if (!row.equals(previous)) data.put("shops", row);
        positions(server).add(position);
    }

    public static boolean remove(
            MinecraftServer server, ResourceLocation dimension, BlockPos block) {
        return remove(server, new Position(dimension, block.immutable()));
    }

    private static boolean remove(MinecraftServer server, Position position) {
        var data = SmpData.get(server);
        var removed = data.remove("shops", position.id());
        DIRTY.computeIfAbsent(server, key -> new LinkedHashSet<>()).remove(position);
        positions(server).remove(position);
        if (removed == null) return false;
        for (var profile : data.all("profiles")) {
            var favorites = profile.getCompound("shopFavorites");
            if (!favorites.contains(position.id().toString())) continue;
            favorites.remove(position.id().toString());
            profile.put("shopFavorites", favorites);
            data.changed(profile);
        }
        SmpNetwork.shopRemoved(server, position.id());
        return true;
    }

    private static Position position(CompoundTag row) {
        var dimension = ResourceLocation.tryParse(row.getString("dimension"));
        return dimension == null || !row.contains("position", Tag.TAG_LONG)
                ? null
                : new Position(dimension, BlockPos.of(row.getLong("position")));
    }

    private static void validateLoaded(MinecraftServer server, CompoundTag row) {
        var position = position(row);
        if (position == null) return;
        var level =
                server.getLevel(ResourceKey.create(Registries.DIMENSION, position.dimension()));
        if (level != null && level.hasChunkAt(position.block())) refresh(server, position);
    }

    public static CompoundTag view(ServerPlayer player, SmpNetwork.Query query) {
        var loaded =
                SmpData.get(player.server).all("shops").stream()
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
        var result = new CompoundTag();
        result.putString("tab", "shops");
        result.putString("filter", query.filter());
        result.putInt("page", query.page());
        result.putUUID("viewer", player.getUUID());
        result.putInt("pageSize", SmpConfig.PAGE_SIZE.get());
        String[] options = query.filter().split("\\|", 2);
        String section = options[0], sort = options.length > 1 ? options[1] : options[0];
        var favorites = Profiles.get(player.server, player.getUUID()).getCompound("shopFavorites");
        String search = query.search().toLowerCase(Locale.ROOT);
        var matches =
                SmpData.get(player.server).all("shops").stream()
                        .filter(
                                row ->
                                        switch (section) {
                                            case "favorites" ->
                                                    favorites.getBoolean(
                                                            row.getUUID("id").toString());
                                            case "mine" ->
                                                    row.hasUUID("owner")
                                                            && row.getUUID("owner")
                                                                    .equals(player.getUUID());
                                            default -> true;
                                        })
                        .filter(row -> matches(row, search))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Comparator<CompoundTag> order = Comparator.comparing(row -> row.getUUID("id").toString());
        if (sort.equals("near"))
            order =
                    Comparator.comparingDouble((CompoundTag row) -> distance(player, row))
                            .thenComparing(order);
        if (sort.equals("price") && !matches.isEmpty()) {
            var payment = ItemStack.of(matches.get(0).getCompound("payment"));
            var offer = ItemStack.of(matches.get(0).getCompound("offer"));
            boolean comparable =
                    !payment.isEmpty()
                            && !offer.isEmpty()
                            && matches.stream()
                                    .allMatch(
                                            row ->
                                                    !row.getBoolean("selectable")
                                                            && row.getInt("quantity") > 0
                                                            && ItemStack.isSameItemSameTags(
                                                                    payment,
                                                                    ItemStack.of(
                                                                            row.getCompound(
                                                                                    "payment")))
                                                            && ItemStack.isSameItemSameTags(
                                                                    offer,
                                                                    ItemStack.of(
                                                                            row.getCompound(
                                                                                    "offer"))));
            result.putBoolean("priceComparable", comparable);
            if (comparable)
                order =
                        Comparator.comparingDouble(
                                        (CompoundTag row) ->
                                                (double) row.getInt("price")
                                                        / row.getInt("quantity"))
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
            var selected = SmpData.get(player.server).find("shops", query.selected());
            if (selected != null) {
                validateLoaded(player.server, selected);
                selected = SmpData.get(player.server).find("shops", query.selected());
            }
            if (selected == null)
                selected =
                        matches.stream()
                                .filter(row -> row.getUUID("id").equals(query.selected()))
                                .findFirst()
                                .orElse(null);
            if (selected != null) {
                var detail = publicView(player, selected);
                var actions = new ListTag();
                actions.add(StringTag.valueOf("favorite"));
                actions.add(StringTag.valueOf("navigate"));
                detail.put("actions", actions);
                detail.putBoolean("favorite", favorites.getBoolean(query.selected().toString()));
                result.put("detail", detail);
            }
        }
        return result;
    }

    private static boolean matches(CompoundTag row, String search) {
        String seller = "", wanted = search.trim();
        if (search.contains("\u001f")) {
            String[] parts = search.split("\u001f", 2);
            seller = parts[0].trim();
            wanted = parts[1].trim();
        } else if (search.startsWith("player:")) {
            seller = search.substring("player:".length()).trim();
            wanted = "";
        } else if (search.startsWith("item:")) wanted = search.substring("item:".length()).trim();
        if (!row.getString("name").toLowerCase(Locale.ROOT).contains(seller)) return false;
        var item = ItemStack.of(row.getCompound("offer"));
        String offered = row.getString("title") + " " + item.getHoverName().getString();
        for (Tag value : row.getList("choices", Tag.TAG_COMPOUND)) {
            var choice = ItemStack.of((CompoundTag) value);
            offered += " " + choice.getHoverName().getString();
        }
        return offered.toLowerCase(Locale.ROOT).contains(wanted);
    }

    private static CompoundTag publicView(ServerPlayer player, CompoundTag row) {
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
            if (row.contains(key)) result.put(key, row.get(key).copy());
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
            var stack = boundedStack(row.getCompound(key), remaining);
            remaining -= stack.sizeInBytes();
            result.put(key, stack);
            if (!stack.equals(row.getCompound(key)))
                result.putBoolean("itemDetailsTruncated", true);
        }
        var choices = new ListTag();
        for (Tag value : row.getList("choices", Tag.TAG_COMPOUND)) {
            var stack = boundedStack((CompoundTag) value, remaining);
            remaining -= stack.sizeInBytes();
            choices.add(stack);
            if (!stack.equals(value)) result.putBoolean("itemDetailsTruncated", true);
        }
        if (!choices.isEmpty()) result.put("choices", choices);
        var offers = new ListTag();
        for (Tag value : row.getList("offers", Tag.TAG_COMPOUND)) {
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
        if (row.hasUUID("owner")) result.putBoolean("ownerOnline", player.server.getPlayerList().getPlayer(row.getUUID("owner")) != null);
        if (!row.contains("dimension")) return result;
        var level =
                player.server.getLevel(
                        ResourceKey.create(
                                Registries.DIMENSION,
                                new ResourceLocation(row.getString("dimension"))));
        var block = BlockPos.of(row.getLong("position"));
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

    private static double distance(ServerPlayer player, CompoundTag row) {
        return player.level().dimension().location().toString().equals(row.getString("dimension"))
                ? player.blockPosition().distSqr(BlockPos.of(row.getLong("position")))
                : Double.POSITIVE_INFINITY;
    }

    public static void action(ServerPlayer player, UUID id, String action) {
        var row = id == null ? null : SmpData.get(player.server).find("shops", id);
        if (id == null) throw new IllegalArgumentException("unavailable");
        if (action.equals("favorite")) {
            var profile = Profiles.get(player.server, player.getUUID());
            var favorites = profile.getCompound("shopFavorites");
            if (favorites.getBoolean(id.toString())) favorites.remove(id.toString());
            else {
                if (row == null) throw new IllegalArgumentException("unavailable");
                if (favorites.size() >= SmpConfig.SHOP_FAVORITES_LIMIT.get())
                    throw new IllegalArgumentException("favorite_limit");
                favorites.putBoolean(id.toString(), true);
            }
            profile.put("shopFavorites", favorites);
            SmpData.get(player.server).changed(profile);
            return;
        }
        if (row != null && action.equals("buy")) {
            validateLoaded(player.server, row);
            row = SmpData.get(player.server).find("shops", id);
            if (row == null) throw new IllegalArgumentException("unavailable");
            if (!net.minecraftforge.fml.ModList.get().isLoaded("spudaciousshops")) throw new IllegalArgumentException("unavailable");
            RemoteShop.open(player, row);
            return;
        }
        if (row == null || !action.equals("navigate"))
            throw new IllegalArgumentException("unavailable");
        Navigation.send(
                player,
                "shops",
                id,
                row.getString("title"),
                new ResourceLocation(row.getString("dimension")),
                BlockPos.of(row.getLong("position")),
                System.currentTimeMillis()
                        + java.util.concurrent.TimeUnit.MINUTES.toMillis(
                                SmpConfig.WAYPOINT_MINUTES.get()));
    }

    private ShopIndex() {}
}
