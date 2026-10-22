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
@Mod.EventBusSubscriber(modid="jem_roles", bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SandboxNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("jem_visual_sandbox", "skills"), () -> "9",
            v -> "9".equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v),
            v -> "9".equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v));
    private sealed interface InputPayload permits ChooseRace, UnlockBloom, RaceControl, DevelopmentControl {
        void apply(ServerPlayer player);
    }
    private record ChooseRace(com.siirio.jemroles.race.RaceId race, java.util.UUID nonce) implements InputPayload {
        private ChooseRace { java.util.Objects.requireNonNull(race); java.util.Objects.requireNonNull(nonce); }
        public void apply(ServerPlayer player) { RaceExperience.choose(player, race.ordinal(), nonce); }
    }
    private record UnlockBloom(int stage) implements InputPayload {
        private UnlockBloom { if(stage<0||stage>3)throw new IllegalArgumentException("Invalid bloom stage"); }
        public void apply(ServerPlayer player) { RaceExperience.upgrade(player, stage); }
    }
    private enum ControlAction { CAST, STATUS, PRESS, RELEASE, OPEN_SELECTION_SCENE, CLOSE_SELECTION_SCENE }
    private record RaceControl(ControlAction action) implements InputPayload {
        public void apply(ServerPlayer player) {
            switch(action) {
                case CAST -> RaceExperience.cast(player);
                case STATUS -> status(player);
                case PRESS -> RaceExperience.utility(player,true);
                case RELEASE -> RaceExperience.utility(player,false);
                case OPEN_SELECTION_SCENE -> RaceSelectionScene.setEnabled(player,true);
                case CLOSE_SELECTION_SCENE -> RaceSelectionScene.setEnabled(player,false);
            }
        }
    }
    private record DevelopmentControl(int action,int value) implements InputPayload {
        private DevelopmentControl {
            if(action<7||action>9)throw new IllegalArgumentException("Invalid development action");
        }
        public void apply(ServerPlayer player) { RaceDevelopment.handle(player,action,value); }
    }
    /** The v9 wire layout and message ID remain unchanged; only this codec sees numeric actions. */
    private record Input(InputPayload payload) {
        static Input decode(FriendlyByteBuf buffer) {
            int action=buffer.readVarInt(),value=buffer.readVarInt();
            return new Input(switch(action) {
                case 0 -> {
                    if(value<0||value>=5)throw new IllegalArgumentException("Invalid race");
                    yield new ChooseRace(com.siirio.jemroles.race.RaceId.values()[value],buffer.readUUID());
                }
                case 1 -> new RaceControl(ControlAction.CAST);
                case 2 -> new RaceControl(ControlAction.STATUS);
                case 3 -> new RaceControl(ControlAction.PRESS);
                case 4 -> new RaceControl(ControlAction.RELEASE);
                case 5 -> new UnlockBloom(value);
                case 6 -> {
                    if(value!=0&&value!=1)throw new IllegalArgumentException("Invalid scene control");
                    yield new RaceControl(value==1?ControlAction.OPEN_SELECTION_SCENE:ControlAction.CLOSE_SELECTION_SCENE);
                }
                case 7,8,9 -> new DevelopmentControl(action,value);
                default -> throw new IllegalArgumentException("Unknown race input");
            });
        }
        void encode(FriendlyByteBuf buffer) {
            if(payload instanceof ChooseRace choose) {
                buffer.writeVarInt(0);buffer.writeVarInt(choose.race().ordinal());buffer.writeUUID(choose.nonce());
            } else if(payload instanceof UnlockBloom unlock) {
                buffer.writeVarInt(5);buffer.writeVarInt(unlock.stage());
            } else if(payload instanceof DevelopmentControl development) {
                buffer.writeVarInt(development.action());buffer.writeVarInt(development.value());
            } else if(payload instanceof RaceControl control) {
                int action=switch(control.action()) {
                    case CAST -> 1; case STATUS -> 2; case PRESS -> 3; case RELEASE -> 4;
                    case OPEN_SELECTION_SCENE,CLOSE_SELECTION_SCENE -> 6;
                };
                buffer.writeVarInt(action);
                buffer.writeVarInt(control.action()==ControlAction.OPEN_SELECTION_SCENE?1:0);
            }
        }
        void handle(Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player=context.getSender();
                if(player!=null&&RaceExperience.allowed(player))payload.apply(player);
            });
            context.setPacketHandled(true);
        }
    }
    public record Status(RaceExperience.Status value) {
        static Status decode(FriendlyByteBuf b){
            int race=b.readVarInt();boolean selected=b.readBoolean();int[] tiers=new int[5],progress=new int[5];
            if(race< -1 || race>4)throw new IllegalArgumentException("Invalid race snapshot");
            for(int i=0;i<5;i++){tiers[i]=b.readVarInt();progress[i]=b.readVarInt();if(tiers[i]<0||tiers[i]>3||progress[i]<0||progress[i]>RacePowerStages.requiredUses(3))throw new IllegalArgumentException("Invalid mastery snapshot");}
            long signature=b.readVarLong(),secondary=b.readVarLong();boolean developer=b.readBoolean();int bloom=b.readVarInt();
            if(signature<0||secondary<0||bloom<0||bloom>3)throw new IllegalArgumentException("Invalid cooldown snapshot");
            long reselect=b.readVarLong();boolean test=b.readBoolean();java.util.UUID nonce=b.readUUID();
            if(reselect<0)throw new IllegalArgumentException("Invalid selection wait");
            return new Status(new RaceExperience.Status(race,selected,tiers,progress,signature,secondary,developer,bloom,reselect,test,nonce,b.readBoolean()));
        }
        void encode(FriendlyByteBuf b){b.writeVarInt(value.race());b.writeBoolean(value.selected());for(int i=0;i<5;i++){b.writeVarInt(value.tierAt(i));b.writeVarInt(value.progressAt(i));}b.writeVarLong(value.signatureRemaining());b.writeVarLong(value.secondaryRemaining());b.writeBoolean(value.developerMode());b.writeVarInt(value.bloomUpgrade());b.writeVarLong(value.reselectRemaining());b.writeBoolean(value.testProfile());b.writeUUID(value.nonce());b.writeBoolean(value.healingPenalty());}
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
    public record SculkScale(int entityId,boolean enlarged) {
        static SculkScale decode(FriendlyByteBuf b){return new SculkScale(b.readVarInt(),b.readBoolean());}
        void encode(FriendlyByteBuf b){b.writeVarInt(entityId);b.writeBoolean(enlarged);}
        void handle(Supplier<NetworkEvent.Context> supplier){
            var context=supplier.get();context.enqueueWork(()->com.siirio.jemvisualsandbox.client.races.SculkScaleClient.receive(this));context.setPacketHandled(true);
        }
    }
    public record Open() {
        void handle(Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.openSelection(false));c.setPacketHandled(true);}
    }
    public static void open(ServerPlayer p){if(CHANNEL.isRemotePresent(p.connection.connection))CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Open());}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(Open.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{}).decoder(b->new Open()).consumerMainThread(Open::handle).add();
            CHANNEL.messageBuilder(Input.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Input::encode).decoder(Input::decode).consumerMainThread(Input::handle).add();
            CHANNEL.messageBuilder(Visual.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(Visual::encode).decoder(Visual::decode).consumerMainThread(Visual::handle).add();
            CHANNEL.messageBuilder(Path.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(Path::encode).decoder(Path::decode).consumerMainThread(Path::handle).add();
            CHANNEL.messageBuilder(Status.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder(Status::encode).decoder(Status::decode).consumerMainThread(Status::handle).add();
            CHANNEL.messageBuilder(SculkScale.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder(SculkScale::encode).decoder(SculkScale::decode).consumerMainThread(SculkScale::handle).add();
        });
    }
    public static void select(int race,java.util.UUID nonce) { CHANNEL.sendToServer(new Input(new ChooseRace(com.siirio.jemroles.race.RaceId.values()[race],nonce))); }
    public static void sync() { CHANNEL.sendToServer(new Input(new RaceControl(ControlAction.STATUS))); }
    public static void upgrade(int next) { CHANNEL.sendToServer(new Input(new UnlockBloom(next))); }
    public static void cinematic(boolean enabled){CHANNEL.sendToServer(new Input(new RaceControl(enabled?ControlAction.OPEN_SELECTION_SCENE:ControlAction.CLOSE_SELECTION_SCENE)));}
    public static void developer(boolean enabled){RaceDevelopment.request(7,enabled?1:0);}
    public static void developerTier(int tier){RaceDevelopment.request(8,tier);}
    public static void developerReset(){RaceDevelopment.request(9,0);}
    static void developmentInput(int action,int value){CHANNEL.sendToServer(new Input(new DevelopmentControl(action,value)));}
    public static boolean supported(net.minecraft.network.Connection connection){return CHANNEL.isRemotePresent(connection);}
    static void sendStatus(ServerPlayer p,RaceExperience.Status value){
        if(CHANNEL.isRemotePresent(p.connection.connection))CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Status(value));
    }
    static void sculkScale(ServerPlayer p,boolean enlarged){
        if(CHANNEL.isRemotePresent(p.connection.connection))CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new SculkScale(p.getId(),enlarged));
    }
    public static void status(ServerPlayer p) {
        RaceExperience.snapshot(p);
    }
    public static void cast() { CHANNEL.sendToServer(new Input(new RaceControl(ControlAction.CAST))); }
    public static void secondary() { CHANNEL.sendToServer(new Input(new RaceControl(ControlAction.PRESS))); }
    public static void secondaryRelease() { CHANNEL.sendToServer(new Input(new RaceControl(ControlAction.RELEASE))); }
    public static void path(ServerPlayer player,int kind,java.util.List<Vec3> points) {
        if(points.size()>1)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Path(kind,points));
    }
    public static void effect(ServerPlayer player,int action,int race,Vec3 origin,float yaw,int subject) {
        for(ServerPlayer viewer:player.serverLevel().players())if(viewer.distanceToSqr(player)<96*96 && CHANNEL.isRemotePresent(viewer.connection.connection))
            CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new Visual(action,race,origin,yaw,subject));
    }
    public static void vine(ServerPlayer player, net.minecraft.world.entity.LivingEntity target) {
        effect(player,10,2,target.position(),0,target.getId());
    }
    public static void visual(ServerPlayer player,int race,Vec3 origin,float yaw) {
        effect(player,1,race,origin,yaw,-1);
    }
    public static void slash(ServerPlayer player, int hit, Vec3 origin, float yaw) {
        if (hit < 0 || hit > 2) throw new IllegalArgumentException("Invalid fiery slash hit");
        effect(player,2+hit,4,origin,yaw,-1);
    }
}
