package com.siirio.jemsettlements.runtime;

import com.siirio.jemsettlements.town.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.sounds.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Loaded-only reconciliation and temporary native chest lid events; never owns inventory. */
public final class SharedStorageService {
    private record Key(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) {}
    private record Opening(long until,boolean audible) {}
    private static final java.util.Map<Key,Opening> OPEN = new java.util.HashMap<>();
    private SharedStorageService() {}
    public static BlockPos partner(ServerLevel level, BlockPos pos) {
        var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE)==ChestType.SINGLE)return null;
        BlockPos other=pos.relative(ChestBlock.getConnectedDirection(state));
        return level.hasChunkAt(other) && level.getBlockState(other).getBlock()==state.getBlock()?other:null;
    }
    private static boolean inventory(ServerLevel level,BlockPos pos) {
        var entity=level.getBlockEntity(pos);return entity!=null && entity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }
    public static void open(ServerLevel level,BlockPos pos,long now) {
        pulse(level,pos,now,true);BlockPos other=partner(level,pos);if(other!=null)pulse(level,other,now,false);
    }
    public static boolean isOpen(net.minecraft.world.level.Level level,BlockPos pos){return !level.isClientSide && OPEN.containsKey(new Key(level.dimension(),pos));}
    private static void pulse(ServerLevel level,BlockPos pos,long now,boolean audible) {
        if(!(level.getBlockEntity(pos) instanceof ChestBlockEntity))return;
        Key key=new Key(level.dimension(),pos.immutable());
        if(!OPEN.containsKey(key) && audible) {
            if(ChestBlockEntity.getOpenCount(level,pos)==0)level.playSound(null,pos,SoundEvents.CHEST_OPEN,SoundSource.BLOCKS,.5F,1F);
            level.gameEvent(null,GameEvent.CONTAINER_OPEN,pos);
        }
        OPEN.put(key,new Opening(now+20,audible)); level.blockEvent(pos,level.getBlockState(pos).getBlock(),1,Math.max(1,ChestBlockEntity.getOpenCount(level,pos)));
    }
    public static void tick(MinecraftServer server) {
        long now=server.overworld().getGameTime();
        OPEN.entrySet().removeIf(entry->{
            ServerLevel level=server.getLevel(entry.getKey().dimension());BlockPos pos=entry.getKey().pos();
            if(level==null || !level.hasChunkAt(pos))return true;
            if(now<entry.getValue().until()) { if(now%5==0)level.blockEvent(pos,level.getBlockState(pos).getBlock(),1,Math.max(1,ChestBlockEntity.getOpenCount(level,pos)));return false; }
            if(level.getBlockEntity(pos) instanceof ChestBlockEntity) {
                int viewers=ChestBlockEntity.getOpenCount(level,pos);level.blockEvent(pos,level.getBlockState(pos).getBlock(),1,viewers);
                if(viewers==0 && entry.getValue().audible()) {level.playSound(null,pos,SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,.5F,1F);level.gameEvent(null,GameEvent.CONTAINER_CLOSE,pos);}
            }
            return true;
        });
        if(now%20!=0)return;
        var data=SettlementSavedData.get(server);
        for(var town:data.towns()) {
            ServerLevel level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,town.dimension()));if(level==null)continue;
            var links=new java.util.ArrayList<StorageLink>();
            for(var link:town.storage().stream().sorted(java.util.Comparator.comparingInt(StorageLink::number)).toList()) {
                if(!link.dimension().equals(town.dimension()) || !level.hasChunkAt(link.position()) || link.second()!=null&&!level.hasChunkAt(link.second())) {links.add(link);continue;}
                BlockPos first=link.position(),other=link.second();
                if(!inventory(level,first)) {if(other==null||!inventory(level,other))continue;first=other;other=null;}
                BlockPos connected=partner(level,first);other=connected;
                StorageLink current=link.at(first,other);
                if(links.stream().noneMatch(existing->existing.contains(current.position())||current.second()!=null&&existing.contains(current.second())))links.add(current);
            }
            if(!links.equals(town.storage())) {town.replaceStorage(links);data.changed();}
            CompoundTag labels=new CompoundTag();labels.putUUID("Town",town.id());labels.putString("Dimension",town.dimension().toString());ListTag rows=new ListTag();
            for(var link:links) {if(!level.hasChunkAt(link.position()))continue;CompoundTag row=new CompoundTag();row.putLong("Position",link.position().asLong());if(link.second()!=null)row.putLong("Second",link.second().asLong());row.putInt("Number",link.number());rows.add(row);}labels.put("Labels",rows);
            for(var player:level.players())if(player.distanceToSqr(town.center().getX()+.5,town.center().getY()+.5,town.center().getZ()+.5)<256D*256D)
                com.siirio.jemsettlements.network.SettlementNetwork.sendStorageLabels(player,labels);
        }
    }
    /** Resolve only the linked physical inventory, including a fully loaded native double chest. */
    public static java.util.Optional<net.minecraftforge.items.IItemHandler> handler(ServerLevel level, StorageLink link) {
        if (level == null || !level.dimension().location().equals(link.dimension()) || !level.hasChunkAt(link.position())
                || link.second() != null && !level.hasChunkAt(link.second())) return java.util.Optional.empty();
        var state = level.getBlockState(link.position());
        if (state.getBlock() instanceof ChestBlock chest) {
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE && partner(level, link.position()) == null) return java.util.Optional.empty();
            if (ChestBlock.getContainer(chest, state, level, link.position(), false) == null) return java.util.Optional.empty();
        }
        var entity = level.getBlockEntity(link.position());
        return entity == null ? java.util.Optional.empty() : entity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
    }
    public static void clear() {OPEN.clear();}

}
