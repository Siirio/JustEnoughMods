package com.siirio.jemroles.network;

import com.siirio.jemroles.vfx.RaceVfxId;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record RaceVfxPacket(
        RaceVfxId id,
        ResourceLocation dimension,
        Vec3 origin,
        Vec3 target,
        int sourceEntityId,
        int targetEntityId,
        int[] relatedEntityIds,
        List<Vec3> path,
        int tier,
        int duration,
        long seed) {
    public static final int MAX_RELATED_ENTITIES = 8;
    public static final int MAX_PATH_POINTS = 16;
    public static final double MAX_OFFSET_SQR = 128.0D * 128.0D;

    public RaceVfxPacket {
        relatedEntityIds = relatedEntityIds == null ? new int[0] : relatedEntityIds.clone();
        path = path == null ? List.of() : List.copyOf(path);
    }

    @Override
    public int[] relatedEntityIds() {
        return relatedEntityIds.clone();
    }

    public static RaceVfxPacket simple(RaceVfxId id, ResourceLocation dimension, Vec3 origin, Vec3 target,
                                       int sourceEntityId, int targetEntityId, int tier, int duration, long seed) {
        return new RaceVfxPacket(id, dimension, origin, target, sourceEntityId, targetEntityId,
                new int[0], List.of(), tier, duration, seed);
    }

    public boolean valid() {
        if (id == null || dimension == null || !finite(origin) || !finite(target)) return false;
        if (tier < 0 || tier > 3 || duration < 1 || duration > 200) return false;
        if (sourceEntityId < -1 || targetEntityId < -1) return false;
        if (relatedEntityIds.length > MAX_RELATED_ENTITIES || path.size() > MAX_PATH_POINTS) return false;
        if (origin.distanceToSqr(target) > MAX_OFFSET_SQR) return false;
        for (int entityId : relatedEntityIds) if (entityId < 0) return false;
        for (Vec3 point : path) {
            if (!finite(point) || origin.distanceToSqr(point) > MAX_OFFSET_SQR) return false;
        }
        return true;
    }

    private static boolean finite(Vec3 value) {
        return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    public static void encode(RaceVfxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.id == null ? -1 : packet.id.ordinal());
        buffer.writeUtf(packet.dimension == null ? "" : packet.dimension.toString(), 128);
        writeVec(buffer, packet.origin);
        writeVec(buffer, packet.target);
        buffer.writeVarInt(packet.sourceEntityId);
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeVarInt(packet.relatedEntityIds.length);
        for (int entityId : packet.relatedEntityIds) buffer.writeVarInt(entityId);
        buffer.writeVarInt(packet.path.size());
        for (Vec3 point : packet.path) writeVec(buffer, point);
        buffer.writeVarInt(packet.tier);
        buffer.writeVarInt(packet.duration);
        buffer.writeLong(packet.seed);
    }

    public static RaceVfxPacket decode(FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        RaceVfxId[] ids = RaceVfxId.values();
        RaceVfxId id = ordinal >= 0 && ordinal < ids.length ? ids[ordinal] : null;
        ResourceLocation dimension = ResourceLocation.tryParse(buffer.readUtf(128));
        Vec3 origin = readVec(buffer);
        Vec3 target = readVec(buffer);
        int sourceEntityId = buffer.readVarInt();
        int targetEntityId = buffer.readVarInt();
        int relatedCount = boundedCount(buffer.readVarInt(), MAX_RELATED_ENTITIES, "related entities");
        int[] related = new int[relatedCount];
        for (int i = 0; i < relatedCount; i++) related[i] = buffer.readVarInt();
        int pathCount = boundedCount(buffer.readVarInt(), MAX_PATH_POINTS, "path points");
        Vec3[] path = new Vec3[pathCount];
        for (int i = 0; i < pathCount; i++) path[i] = readVec(buffer);
        return new RaceVfxPacket(id, dimension, origin, target, sourceEntityId, targetEntityId,
                related, List.of(path), buffer.readVarInt(), buffer.readVarInt(), buffer.readLong());
    }

    private static int boundedCount(int value, int maximum, String field) {
        if (value < 0 || value > maximum) throw new DecoderException("Invalid race VFX " + field + " count: " + value);
        return value;
    }

    private static void writeVec(FriendlyByteBuf buffer, Vec3 value) {
        Vec3 safe = value == null ? Vec3.ZERO : value;
        buffer.writeDouble(safe.x);
        buffer.writeDouble(safe.y);
        buffer.writeDouble(safe.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(RaceVfxPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!packet.valid()) return;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.siirio.jemroles.client.vfx.RaceVfxController.accept(packet));
        });
        context.get().setPacketHandled(true);
    }
}
