package com.siirio.jemrewards;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

public final class RewardsNetwork {
    static final int PAGE_SIZE = 8;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("jem_rewards", "main"), () -> "1", "1"::equals, "1"::equals);
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private record Session(UUID token, int page, long revision, long touched) {}
    public record Request(int action, UUID session, long revision, int page, UUID bundle, int item) {
        static Request read(FriendlyByteBuf b) { return new Request(b.readVarInt(),b.readUUID(),b.readLong(),b.readVarInt(),b.readUUID(),b.readVarInt()); }
        static void write(Request p, FriendlyByteBuf b) { b.writeVarInt(p.action); b.writeUUID(p.session); b.writeLong(p.revision); b.writeVarInt(p.page); b.writeUUID(p.bundle); b.writeVarInt(p.item); }
    }
    public record Snapshot(UUID session, long revision, int page, int pages, ListTag rows) {
        static Snapshot read(FriendlyByteBuf b) { UUID id=b.readUUID();long rev=b.readLong();int page=b.readVarInt(),pages=b.readVarInt();CompoundTag data=b.readNbt();return new Snapshot(id,rev,page,pages,data==null?new ListTag():data.getList("Rows",Tag.TAG_COMPOUND)); }
        static void write(Snapshot p, FriendlyByteBuf b) { b.writeUUID(p.session);b.writeLong(p.revision);b.writeVarInt(p.page);b.writeVarInt(p.pages);CompoundTag data=new CompoundTag();data.put("Rows",p.rows);b.writeNbt(data); }
    }
    static void register() {
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Request::write).decoder(Request::read).consumerMainThread((packet, context) -> {
            ServerPlayer player=context.get().getSender();if(player!=null) {
                try { handle(player,packet); }
                catch (RuntimeException failure) {
                    com.mojang.logging.LogUtils.getLogger().error("Reward operation retained for {}",player.getUUID(),failure);
                    player.connection.disconnect(net.minecraft.network.chat.Component.translatable("jem_rewards.storage_unavailable"));
                }
            }
            context.get().setPacketHandled(true);
        }).add();
        CHANNEL.messageBuilder(Snapshot.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(Snapshot::write).decoder(Snapshot::read).consumerMainThread((packet, context) -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemrewards.client.RewardsPanel.accept(packet));context.get().setPacketHandled(true);
        }).add();
    }
    public static void send(Request request) { CHANNEL.sendToServer(request); }
    static void clear() { SESSIONS.clear(); }
    private static void handle(ServerPlayer player, Request request) {
        if (request.action==3) {
            Session existing=SESSIONS.get(player.getUUID());
            if(existing!=null && existing.token.equals(request.session))SESSIONS.remove(player.getUUID());
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) { SESSIONS.remove(player.getUUID()); return; }
        if(request.action<0 || request.action>2 || request.page<0 || request.page>100000 || request.item<0 || request.item>100000) return;
        long now=player.server.overworld().getGameTime();
        Session session=SESSIONS.get(player.getUUID());
        if(session!=null && now-session.touched<2) return;
        RewardsData data=RewardsData.get(player.server);data.recover(player);
        if(request.action!=0 && (session==null || !session.token.equals(request.session) || now-session.touched>1200 || session.revision!=request.revision || data.revision()!=request.revision)) { snapshot(player,0,now);return; }
        if(request.action==1) {
            // Selection must exist on the exact bounded page that the server issued.
            ListTag rows=rows(player,session.page);
            boolean found=false;
            for(Tag value:rows) { CompoundTag row=(CompoundTag)value;if(row.getUUID("Bundle").equals(request.bundle) && row.getInt("Item")==request.item) {found=true;break;} }
            if(found) PendingRewardContainer.claimItem(player,request.bundle,request.item);
        }
        snapshot(player,request.action==0 || request.action==2?request.page:session.page,now);
    }
    private static ListTag rows(ServerPlayer player,int page) {
        ListTag result=new ListTag();int cursor=0,start=page*PAGE_SIZE;
        for(Tag value:PendingRewardContainer.bundles(player,"")) {
            CompoundTag bundle=(CompoundTag)value;ListTag items=bundle.getList("items",Tag.TAG_COMPOUND);
            for(int i=0;i<items.size();i++) { if(cursor++<start)continue;if(result.size()>=PAGE_SIZE)return result;
                CompoundTag row=new CompoundTag();row.putUUID("Bundle",bundle.getUUID("id"));row.putInt("Item",i);row.put("Stack",items.getCompound(i).copy());result.add(row);
            }
        }
        return result;
    }
    private static void snapshot(ServerPlayer player,int requested,long now) {
        int count=0;for(Tag value:PendingRewardContainer.bundles(player,""))count+=((CompoundTag)value).getList("items",Tag.TAG_COMPOUND).size();
        int pages=Math.max(1,(count+PAGE_SIZE-1)/PAGE_SIZE),page=Math.min(requested,pages-1);
        UUID token=UUID.randomUUID();long revision=RewardsData.get(player.server).revision();
        SESSIONS.put(player.getUUID(),new Session(token,page,revision,now));
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Snapshot(token,revision,page,pages,rows(player,page)));
    }
}
