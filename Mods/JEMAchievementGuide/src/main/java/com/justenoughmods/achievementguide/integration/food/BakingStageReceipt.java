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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Pending native tool preparation belongs to this chunk and physical pastry position. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class BakingStageReceipt {
    private static final int LIMIT = 256;
    private static final Capability<Store> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private BakingStageReceipt() {}
    public record Stage(UUID actor, String state, int prepared) {}
    public static final class Store { private final Map<Long, Stage> stages = new HashMap<>(); }
    @Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void registerBakingSurface(RegisterCapabilitiesEvent event) { event.register(Store.class); }
    }
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!ModList.get().isLoaded("bakery") || event.getObject().getLevel().isClientSide) return;
        Provider provider = new Provider(event.getObject());
        event.addCapability(new ResourceLocation(JemAdvancementGuide.MOD_ID, "baking_surface_receipts"), provider);
        event.addListener(provider.value::invalidate);
    }
    private static LevelChunk chunk(Level level, BlockPos pos) {
        return level.isClientSide || !level.hasChunkAt(pos) ? null : level.getChunkAt(pos);
    }
    public static Stage current(Level level, BlockPos pos, BlockState expected) {
        LevelChunk chunk = chunk(level, pos);
        Store store = chunk == null ? null : chunk.getCapability(CAP).orElse(null);
        Stage stage = store == null ? null : store.stages.get(pos.asLong());
        return stage != null && stage.state.equals(expected.toString()) ? stage : null;
    }
    public static void prepared(ServerPlayer actor, BlockPos pos, BlockState state, int prepared) {
        LevelChunk chunk = chunk(actor.level(), pos);
        Store store = chunk == null ? null : chunk.getCapability(CAP).orElse(null);
        if (store == null || prepared < 1 || prepared > 2 || store.stages.size() >= LIMIT) return;
        store.stages.put(pos.asLong(), new Stage(actor.getUUID(), state.toString(), prepared));
        chunk.setUnsaved(true);
    }
    public static void invalidate(LevelChunk chunk, BlockPos pos) {
        Store store = chunk.getCapability(CAP).orElse(null);
        if (store != null && store.stages.remove(pos.asLong()) != null) chunk.setUnsaved(true);
    }
    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final LevelChunk chunk;
        private final Store store = new Store();
        private final LazyOptional<Store> value = LazyOptional.of(() -> store);
        private Provider(LevelChunk chunk) { this.chunk = chunk; }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) { return capability == CAP ? value.cast() : LazyOptional.empty(); }
        @Override public CompoundTag serializeNBT() {
            CompoundTag result = new CompoundTag();
            store.stages.forEach((pos, stage) -> {
                CompoundTag row = new CompoundTag();
                row.putUUID("actor", stage.actor);
                row.putString("state", stage.state);
                row.putInt("prepared", stage.prepared);
                result.put(Long.toString(pos), row);
            });
            return result;
        }
        @Override public void deserializeNBT(CompoundTag tag) {
            store.stages.clear();
            for (String key : tag.getAllKeys()) {
                if (store.stages.size() >= LIMIT) break;
                try {
                    long packed = Long.parseLong(key);
                    BlockPos pos = BlockPos.of(packed);
                    CompoundTag row = tag.getCompound(key);
                    int prepared = row.getInt("prepared");
                    String state = row.getString("state");
                    if ((pos.getX() >> 4) == chunk.getPos().x && (pos.getZ() >> 4) == chunk.getPos().z
                            && prepared >= 1 && prepared <= 2 && row.hasUUID("actor") && !state.isEmpty() && state.length() < 512)
                        store.stages.put(packed, new Stage(row.getUUID("actor"), state, prepared));
                } catch (IllegalArgumentException ignored) { }
            }
        }
    }
}
