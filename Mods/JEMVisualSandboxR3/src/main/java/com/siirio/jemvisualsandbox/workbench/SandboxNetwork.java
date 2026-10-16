package com.siirio.jemvisualsandbox.workbench;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Sandbox input is validated and executed by its integrated server. */
@Mod.EventBusSubscriber(modid="jem_visual_sandbox", bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SandboxNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("jem_visual_sandbox", "skills"), () -> "1", "1"::equals, "1"::equals);
    public record Input(int action, int race) {
        static Input decode(FriendlyByteBuf buffer) { return new Input(buffer.readVarInt(), buffer.readVarInt()); }
        void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(action); buffer.writeVarInt(race); }
        void handle(Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || player.server.isDedicatedServer() || !player.server.isSingleplayerOwner(player.getGameProfile())) return;
                if (action == 0 && race >= 0 && race < 5) {
                    player.getPersistentData().putInt("JemSandboxRace", race);
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Visual(0, race, Vec3.ZERO, 0));
                } else if (action == 1) SandboxSkills.cast(player, player.getPersistentData().getInt("JemSandboxRace"));
                else if (action == 2) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Visual(0, player.getPersistentData().getInt("JemSandboxRace"), Vec3.ZERO, 0));
            });
            context.setPacketHandled(true);
        }
    }
    public record Visual(int action, int race, Vec3 origin, float yaw) {
        static Visual decode(FriendlyByteBuf b) { return new Visual(b.readVarInt(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()); }
        void encode(FriendlyByteBuf b) { b.writeVarInt(action);b.writeVarInt(race);b.writeDouble(origin.x);b.writeDouble(origin.y);b.writeDouble(origin.z);b.writeFloat(yaw); }
        void handle(Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(() -> BlockbenchWorkbench.receive(this));
            context.setPacketHandled(true);
        }
    }
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(Input.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Input::encode).decoder(Input::decode).consumerMainThread(Input::handle).add();
            CHANNEL.messageBuilder(Visual.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(Visual::encode).decoder(Visual::decode).consumerMainThread(Visual::handle).add();
        });
    }
    public static void select(int race) { CHANNEL.sendToServer(new Input(0,race)); }
    public static void sync() { CHANNEL.sendToServer(new Input(2,0)); }
    public static void cast() { CHANNEL.sendToServer(new Input(1,0)); }
    public static void visual(ServerPlayer player,int race,Vec3 origin,float yaw) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Visual(1,race,origin,yaw));
    }
}
