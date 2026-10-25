package com.justenoughmods.achievementguide.handbook;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Projects the existing world discovery owner. No handbook progression is saved. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class HandbookWorldAccess {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JemAdvancementGuide.MOD_ID, "handbook_access"), () -> "1", "1"::equals, "1"::equals);
    private static final Map<ServerPlayer, Snapshot> SENT = new WeakHashMap<>();

    private HandbookWorldAccess() { }

    public static void register() {
        CHANNEL.registerMessage(0, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) return;
        Snapshot state = read(player.server);
        if (!state.equals(SENT.put(player, state))) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity());
    }

    private static Snapshot read(MinecraftServer server) {
        // Standalone Guide has no JEMServer world discovery gate.
        if (!ModList.get().isLoaded("jem_server")) return new Snapshot(true, true);
        try {
            Class<?> owner = Class.forName("com.siirio.jemserver.smp.events.DimensionProgress");
            Object state = owner.getMethod("get", MinecraftServer.class).invoke(null, server);
            return new Snapshot((Boolean) owner.getMethod("netherUnlocked").invoke(state),
                    (Boolean) owner.getMethod("endUnlocked").invoke(state));
        } catch (InvocationTargetException error) {
            throw new IllegalStateException("Handbook dimension discovery owner failed", error.getCause());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Handbook dimension discovery API changed", error);
        }
    }

    private record Snapshot(boolean nether, boolean end) {
        private static void encode(Snapshot state, FriendlyByteBuf buffer) {
            buffer.writeBoolean(state.nether);
            buffer.writeBoolean(state.end);
        }

        private static Snapshot decode(FriendlyByteBuf buffer) {
            return new Snapshot(buffer.readBoolean(), buffer.readBoolean());
        }

        private static void handle(Snapshot state, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                if (ModList.get().isLoaded("patchouli")) DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.justenoughmods.achievementguide.client.handbook.HandbookWorldVisibility.update(state.nether, state.end));
            });
            context.setPacketHandled(true);
        }
    }
}
