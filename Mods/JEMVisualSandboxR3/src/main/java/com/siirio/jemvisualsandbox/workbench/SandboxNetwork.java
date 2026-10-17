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
            new ResourceLocation("jem_visual_sandbox", "skills"), () -> "4",
            v -> "4".equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v),
            v -> "4".equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v));
    public record Input(int action, int race) {
        static Input decode(FriendlyByteBuf buffer) { return new Input(buffer.readVarInt(), buffer.readVarInt()); }
        void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(action); buffer.writeVarInt(race); }
        void handle(Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !RaceExperience.allowed(player) && !ProductionRaceBridge.available(player)) return;
                if (action == 0 && race >= 0 && race < 5) {
                    RaceExperience.choose(player,race);
                } else if (action == 1) RaceExperience.cast(player);
                else if (action == 3) RaceExperience.utility(player,true);
                else if (action == 4) RaceExperience.utility(player,false);
                else if (action == 2) status(player);
                else if (action == 5) RaceExperience.upgrade(player,race);
                else if (action == 6) RaceExperience.cinematic(player,race==1);
                else if (action == 7) RaceExperience.developer(player,race==1);
                else if (action == 8) RaceExperience.developerTier(player,race);
            });
            context.setPacketHandled(true);
        }
    }
    public record Status(RaceExperience.Status value) {
        static Status decode(FriendlyByteBuf b){
            int race=b.readVarInt();boolean selected=b.readBoolean();int[] tiers=new int[5],progress=new int[5];
            if(race< -1 || race>4)throw new IllegalArgumentException("Invalid race snapshot");
            for(int i=0;i<5;i++){tiers[i]=b.readVarInt();progress[i]=b.readVarInt();if(tiers[i]<1||tiers[i]>3||progress[i]<0||progress[i]>30)throw new IllegalArgumentException("Invalid mastery snapshot");}
            long signature=b.readVarLong(),secondary=b.readVarLong();boolean developer=b.readBoolean();int bloom=b.readVarInt();
            if(signature<0||secondary<0||bloom<0||bloom>3)throw new IllegalArgumentException("Invalid cooldown snapshot");
            return new Status(new RaceExperience.Status(race,selected,tiers,progress,signature,secondary,developer,bloom));
        }
        void encode(FriendlyByteBuf b){b.writeVarInt(value.race());b.writeBoolean(value.selected());for(int i=0;i<5;i++){b.writeVarInt(value.tiers()[i]);b.writeVarInt(value.progress()[i]);}b.writeVarLong(value.signatureRemaining());b.writeVarLong(value.secondaryRemaining());b.writeBoolean(value.developerMode());b.writeVarInt(value.bloomUpgrade());}
        void handle(Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->{
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->{
                com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.receive(value);
                com.siirio.jemvisualsandbox.client.races.OptionalRacePresentation.accept(value);
                BlockbenchWorkbench.acceptRace(value);
            });
        });c.setPacketHandled(true);}
    }
    public record Path(int kind, java.util.List<Vec3> points) {
        public Path { points=java.util.List.copyOf(points);if(points.size()>128)throw new IllegalArgumentException("Path too long"); }
        static Path decode(FriendlyByteBuf b) {
            int kind=b.readVarInt(),count=b.readVarInt();if(kind<0 || kind>1 || count<0 || count>128)throw new IllegalArgumentException("Invalid sandbox path");
            var points=new java.util.ArrayList<Vec3>();for(int i=0;i<count;i++)points.add(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));return new Path(kind,points);
        }
        void encode(FriendlyByteBuf b) { b.writeVarInt(kind);b.writeVarInt(points.size());for(Vec3 p:points){b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);} }
        void handle(Supplier<NetworkEvent.Context> supplier) { var c=supplier.get();c.enqueueWork(()->SurfaceVfx.receive(this));c.setPacketHandled(true); }
    }
    public record Visual(int action, int race, Vec3 origin, float yaw, int subject) {
        public Visual(int action,int race,Vec3 origin,float yaw) { this(action,race,origin,yaw,-1); }
        static Visual decode(FriendlyByteBuf b) { return new Visual(b.readVarInt(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readVarInt()); }
        void encode(FriendlyByteBuf b) { b.writeVarInt(action);b.writeVarInt(race);b.writeDouble(origin.x);b.writeDouble(origin.y);b.writeDouble(origin.z);b.writeFloat(yaw);b.writeVarInt(subject); }
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
            CHANNEL.messageBuilder(Path.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(Path::encode).decoder(Path::decode).consumerMainThread(Path::handle).add();
            CHANNEL.messageBuilder(Status.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder(Status::encode).decoder(Status::decode).consumerMainThread(Status::handle).add();
        });
    }
    public static void select(int race) { CHANNEL.sendToServer(new Input(0,race)); }
    public static void sync() { CHANNEL.sendToServer(new Input(2,0)); }
    public static void upgrade(int next) { CHANNEL.sendToServer(new Input(5,next)); }
    public static void cinematic(boolean enabled){CHANNEL.sendToServer(new Input(6,enabled?1:0));}
    public static void developer(boolean enabled){CHANNEL.sendToServer(new Input(7,enabled?1:0));}
    public static void developerTier(int tier){CHANNEL.sendToServer(new Input(8,tier));}
    public static boolean supported(net.minecraft.network.Connection connection){return CHANNEL.isRemotePresent(connection);}
    static void sendStatus(ServerPlayer p,RaceExperience.Status value){
        if(CHANNEL.isRemotePresent(p.connection.connection))CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Status(value));
    }
    public static void status(ServerPlayer p) {
        RaceExperience.snapshot(p);
    }
    public static void cast() { CHANNEL.sendToServer(new Input(1,0)); }
    public static void secondary() { CHANNEL.sendToServer(new Input(3,0)); }
    public static void secondaryRelease() { CHANNEL.sendToServer(new Input(4,0)); }
    public static void path(ServerPlayer player,int kind,java.util.List<Vec3> points) {
        if(points.size()>1)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Path(kind,points));
    }
    public static void effect(ServerPlayer player,int action,int race,Vec3 origin,float yaw,int subject) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Visual(action,race,origin,yaw,subject));
    }
    public static void vine(ServerPlayer player, net.minecraft.world.entity.LivingEntity target) {
        effect(player,10,2,target.position(),0,target.getId());
    }
    public static void visual(ServerPlayer player,int race,Vec3 origin,float yaw) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Visual(1,race,origin,yaw));
    }
    public static void slash(ServerPlayer player, int hit, Vec3 origin, float yaw) {
        if (hit < 0 || hit > 2) throw new IllegalArgumentException("Invalid fiery slash hit");
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Visual(2 + hit,4,origin,yaw));
    }
}
