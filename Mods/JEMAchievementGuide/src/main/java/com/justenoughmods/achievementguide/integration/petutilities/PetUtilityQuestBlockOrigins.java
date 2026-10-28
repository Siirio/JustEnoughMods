package com.justenoughmods.achievementguide.integration.petutilities;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Native chunk capability stores only observed physical provenance, never quest completion. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class PetUtilityQuestBlockOrigins {
    public static final Capability<PetUtilityQuestBlockOrigins> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private static final int MAX_PER_KIND = 4096;
    private final CompoundTag rows = new CompoundTag();
    private PetUtilityQuestBlockOrigins() {}

    @Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) { event.register(PetUtilityQuestBlockOrigins.class); }
    }

    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<LevelChunk> event) {
        if (ModList.get().isLoaded("golemoverhaul") || ModList.get().isLoaded("ribbits")) {
            Provider provider = new Provider(event.getObject());
            event.addCapability(new ResourceLocation(JemAdvancementGuide.MOD_ID, "pet_utility_origin_v4"), provider);
            event.addListener(provider.optional::invalidate);
        }
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PetUtilityQuestBlockOrigins data = new PetUtilityQuestBlockOrigins();
        private final LazyOptional<PetUtilityQuestBlockOrigins> optional = LazyOptional.of(() -> data);
        private final LevelChunk chunk;
        Provider(LevelChunk chunk) { this.chunk = chunk; }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() { return data.rows.copy(); }
        @Override public void deserializeNBT(CompoundTag tag) {
            data.rows.getAllKeys().stream().toList().forEach(data.rows::remove);
            for (String kind : new String[]{"Placed"}) {
                CompoundTag restored = new CompoundTag();
                CompoundTag saved = tag.getCompound(kind);
                for (String key : saved.getAllKeys()) {
                    if (restored.size() >= MAX_PER_KIND) break;
                    try {
                        BlockPos pos = BlockPos.of(Long.parseLong(key));
                        if (pos.getX() >> 4 == chunk.getPos().x && pos.getZ() >> 4 == chunk.getPos().z
                                && !chunk.isOutsideBuildHeight(pos) && saved.contains(key, 10)) {
                            restored.put(key, saved.getCompound(key).copy());
                        }
                    } catch (NumberFormatException ignored) { /* Invalid provenance never becomes an attribution. */ }
                }
                if (!restored.isEmpty()) data.rows.put(kind, restored);
            }
        }
    }

    public static CompoundTag get(ServerLevel level, BlockPos pos, String kind) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return new CompoundTag();
        var data = chunk.getCapability(CAPABILITY).resolve().orElse(null);
        return data == null ? new CompoundTag() : data.rows.getCompound(kind).getCompound(Long.toString(pos.asLong())).copy();
    }

    public static void put(ServerLevel level, BlockPos pos, String kind, CompoundTag origin) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;
        var data = chunk.getCapability(CAPABILITY).resolve().orElse(null);
        if (data == null) return;
        CompoundTag entries = data.rows.getCompound(kind);
        String key = Long.toString(pos.asLong());
        if (!entries.contains(key) && entries.size() >= MAX_PER_KIND) return;
        entries.put(key, origin.copy());
        data.rows.put(kind, entries);
        chunk.setUnsaved(true);
    }

    public static void remove(LevelChunk chunk, BlockPos pos, boolean changedBlock) {
        var data = chunk.getCapability(CAPABILITY).resolve().orElse(null);
        if (data == null) return;
        String key = Long.toString(pos.asLong());
        for (String kind : new String[]{"Placed"}) {
            if (!changedBlock) continue;
            CompoundTag entries = data.rows.getCompound(kind);
            if (entries.contains(key)) {
                entries.remove(key);
                if (entries.isEmpty()) data.rows.remove(kind);
                chunk.setUnsaved(true);
            }
        }
    }
}
