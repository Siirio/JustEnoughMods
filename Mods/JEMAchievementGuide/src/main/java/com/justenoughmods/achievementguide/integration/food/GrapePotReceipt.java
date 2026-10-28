package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Pending physical pot batches, never quest-completion history. Chunk owns dimension and position. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class GrapePotReceipt {
    private static final int LIMIT = 256;
    private static final Capability<Store> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private GrapePotReceipt() {}
    public record Batch(UUID actor, String state, int portions) {}
    private record Draw(UUID actor, String before, String after) {}
    public static final class Store {
        private final Map<Long, Batch> batches = new HashMap<>();
        private final Map<Long, Draw> draws = new HashMap<>();
    }
    @Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void registerGrapePot(RegisterCapabilitiesEvent event) { event.register(Store.class); }
    }
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!ModList.get().isLoaded("vinery") || event.getObject().getLevel().isClientSide) return;
        Provider provider = new Provider(event.getObject());
        event.addCapability(new ResourceLocation(JemAdvancementGuide.MOD_ID, "grape_pot_receipts"), provider);
        event.addListener(provider.value::invalidate);
    }
    private static boolean real(Player player) {
        return player instanceof ServerPlayer && !(player instanceof FakePlayer) && !player.isSpectator() && !player.getAbilities().instabuild;
    }
    private static Store store(LevelChunk chunk) { return chunk.getCapability(CAP).orElse(null); }
    private static LevelChunk chunk(Level level, BlockPos pos) {
        return level.isClientSide || !level.hasChunkAt(pos) ? null : level.getChunkAt(pos);
    }
    public static void completed(Level level, BlockPos pos, Player player, BlockState state, int portions) {
        LevelChunk chunk = chunk(level, pos);
        if (chunk == null || !real(player) || portions < 1 || portions > 3) return;
        Store store = store(chunk);
        if (store == null || store.batches.size() >= LIMIT) return;
        store.batches.put(pos.asLong(), new Batch(player.getUUID(), state.toString(), portions));
        store.draws.remove(pos.asLong());
        chunk.setUnsaved(true);
    }
    public static Batch beginBottle(Level level, BlockPos pos, BlockState before) {
        LevelChunk chunk = chunk(level, pos);
        Store store = chunk == null ? null : store(chunk);
        if (store == null) return null;
        store.draws.remove(pos.asLong());
        Batch batch = store.batches.remove(pos.asLong());
        if (batch != null) chunk.setUnsaved(true);
        return batch != null && batch.state.equals(before.toString()) ? batch : null;
    }
    public static void bottled(Level level, BlockPos pos, Player player, Batch batch, BlockState before, BlockState after, int remaining) {
        LevelChunk chunk = chunk(level, pos);
        Store store = chunk == null ? null : store(chunk);
        if (store == null || batch == null || batch.portions != remaining + 1) return;
        if (remaining > 0) store.batches.put(pos.asLong(), new Batch(batch.actor, after.toString(), remaining));
        if (real(player) && batch.actor.equals(player.getUUID())) store.draws.put(pos.asLong(), new Draw(batch.actor, before.toString(), after.toString()));
        chunk.setUnsaved(true);
    }
    public static boolean consumeDraw(Level level, BlockPos pos, Player player, BlockState before) {
        LevelChunk chunk = chunk(level, pos);
        Store store = chunk == null ? null : store(chunk);
        if (store == null) return false;
        Draw draw = store.draws.remove(pos.asLong());
        return draw != null && draw.actor.equals(player.getUUID()) && draw.before.equals(before.toString())
                && draw.after.equals(level.getBlockState(pos).toString());
    }
    public static void invalidate(LevelChunk chunk, BlockPos pos) {
        Store store = store(chunk);
        if (store == null) return;
        boolean changed = store.batches.remove(pos.asLong()) != null;
        store.draws.remove(pos.asLong());
        if (changed) chunk.setUnsaved(true);
    }
    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final LevelChunk chunk;
        private final Store store = new Store();
        private final LazyOptional<Store> value = LazyOptional.of(() -> store);
        private Provider(LevelChunk chunk) { this.chunk = chunk; }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) { return capability == CAP ? value.cast() : LazyOptional.empty(); }
        @Override public CompoundTag serializeNBT() {
            CompoundTag result = new CompoundTag();
            store.batches.forEach((pos, batch) -> {
                CompoundTag row = new CompoundTag();
                row.putUUID("actor", batch.actor);
                row.putString("state", batch.state);
                row.putInt("portions", batch.portions);
                result.put(Long.toString(pos), row);
            });
            return result;
        }
        @Override public void deserializeNBT(CompoundTag tag) {
            store.batches.clear();
            store.draws.clear();
            for (String key : tag.getAllKeys()) {
                if (store.batches.size() >= LIMIT) break;
                try {
                    long packed = Long.parseLong(key);
                    BlockPos pos = BlockPos.of(packed);
                    CompoundTag row = tag.getCompound(key);
                    int portions = row.getInt("portions");
                    String state = row.getString("state");
                    if ((pos.getX() >> 4) == chunk.getPos().x && (pos.getZ() >> 4) == chunk.getPos().z
                            && portions > 0 && portions <= 3 && row.hasUUID("actor") && state.length() < 512 && !state.isEmpty())
                        store.batches.put(packed, new Batch(row.getUUID("actor"), state, portions));
                } catch (IllegalArgumentException ignored) { }
            }
        }
    }
}
