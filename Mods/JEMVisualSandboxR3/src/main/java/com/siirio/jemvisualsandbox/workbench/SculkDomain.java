package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One bounded territory per local owner; original ground survives save/reload. */
@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class SculkDomain {
    public static final String SCALE_KEY="JemSandboxSculkDomainScale";
    public static final double RADIUS=16;
    public static final int DURATION=200;
    private record Domain(UUID id,ServerLevel level,Vec3 center,long until) {}
    private record Tile(UUID owner,UUID domain,long until,BlockState original,boolean placed) {}
    private static final Map<UUID,Domain> ACTIVE=new HashMap<>();
    private SculkDomain() {}

    public static void create(ServerPlayer owner,Vec3 center){
        clear(owner);
        ServerLevel level=owner.serverLevel();
        var store=Store.get(level);
        Domain domain=new Domain(UUID.randomUUID(),level,center,level.getGameTime()+DURATION);
        ACTIVE.put(owner.getUUID(),domain);
        BlockPos anchor=BlockPos.containing(center).below();
        // At most797 columns,7 already-loaded surface candidates per column.
        surface: for(int dx=-16;dx<=16;dx++)for(int dz=-16;dz<=16;dz++){
            if(store.tiles.size()>=4096){
                owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Sculk terrain: pending unloaded ground must restore before more is placed"),true);break surface;
            }
            if(dx*dx+dz*dz>RADIUS*RADIUS)continue;
            for(int dy=2;dy>=-4;dy--){
                BlockPos at=anchor.offset(dx,dy,dz);
                if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at))break;
                BlockState state=level.getBlockState(at);
                if(!state.canOcclude()||!level.getBlockState(at.above()).isAir())continue;
                if(state.is(Blocks.SCULK)||state.hasBlockEntity()||!state.getFluidState().isEmpty()
                        ||state.getDestroySpeed(level,at)<0||store.tiles.containsKey(at.asLong()))break;
                store.tiles.put(at.asLong(),new Tile(owner.getUUID(),domain.id,domain.until,state,false));
                store.setDirty();break;
            }
        }
    }

    public static boolean touchesTemporary(net.minecraft.world.level.LevelAccessor world,BlockPos position){
        if(!(world instanceof ServerLevel level)||!RaceExperience.serverActive(level.getServer()))return false;
        var tiles=Store.get(level).tiles;
        Tile tile=tiles.get(position.asLong());if(tile!=null&&tile.placed)return true;
        for(var direction:net.minecraft.core.Direction.values()){
            tile=tiles.get(position.relative(direction).asLong());if(tile!=null&&tile.placed)return true;
        }
        return false;
    }

    public static boolean empowered(ServerPlayer player){
        Domain domain=ACTIVE.get(player.getUUID());
        return domain!=null&&RaceExperience.race(player)==3&&player.isAlive()&&!player.isSpectator()
                &&domain.level==player.serverLevel()&&domain.until>player.level().getGameTime()
                &&Math.abs(player.getY()-domain.center.y)<5
                &&player.position().subtract(domain.center).multiply(1,0,1).lengthSqr()<=RADIUS*RADIUS;
    }

    public static void clear(ServerPlayer owner){
        ACTIVE.remove(owner.getUUID());
        setScale(owner,false);
        // Loaded tiles are restored immediately on a race change or recast.
        for(ServerLevel level:owner.server.getAllLevels())Store.get(level).restoreOwner(level,owner.getUUID());
    }

    private static void setScale(ServerPlayer player,boolean enlarged){
        var data=player.getPersistentData();
        if(data.getBoolean(SCALE_KEY)==enlarged)return;
        if(enlarged){
            var normal=player.getDimensions(player.getPose());
            if(!player.serverLevel().noCollision(player,normal.scale(1.15F).makeBoundingBox(player.position())))return;
        }
        data.putBoolean(SCALE_KEY,enlarged);player.refreshDimensions();
        SandboxNetwork.sculkScale(player,enlarged);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!RaceExperience.serverActive(event.getServer()))return;
        ACTIVE.entrySet().removeIf(entry->{
            Domain domain=entry.getValue();ServerPlayer owner=event.getServer().getPlayerList().getPlayer(entry.getKey());
            return owner==null||!owner.isAlive()||RaceExperience.race(owner)!=3
                    ||owner.serverLevel()!=domain.level||domain.until<=domain.level.getGameTime();
        });
        for(ServerLevel level:event.getServer().getAllLevels())Store.get(level).tick(level);
        for(ServerPlayer player:event.getServer().getPlayerList().getPlayers())if(RaceExperience.allowed(player))setScale(player,empowered(player));
    }

    @SubscribeEvent public static void damage(LivingHurtEvent event){
        if(event.getSource().getEntity() instanceof ServerPlayer player&&RaceExperience.serverActive(player.server)&&empowered(player))
            event.setAmount(event.getAmount()*1.15F);
    }

    @SubscribeEvent public static void size(EntityEvent.Size event){
        if(event.getEntity() instanceof Player player&&player.getPersistentData().getBoolean(SCALE_KEY)
                &&(player.level().isClientSide||player.getServer()!=null&&RaceExperience.serverActive(player.getServer()))){
            event.setNewSize(event.getNewSize().scale(1.15F));
            event.setNewEyeHeight(event.getNewEyeHeight()*1.15F);
        }
    }

    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent event){
        if(event.getLevel() instanceof ServerLevel level&&RaceExperience.serverActive(level.getServer())
                &&Store.get(level).tiles.containsKey(event.getPos().asLong()))event.setCanceled(true);
    }

    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event){
        if(event.getLevel() instanceof ServerLevel level&&RaceExperience.serverActive(level.getServer())){
            var tiles=Store.get(level).tiles;
            event.getAffectedBlocks().removeIf(pos->tiles.containsKey(pos.asLong()));
        }
    }

    @SubscribeEvent public static void piston(PistonEvent.Pre event){
        if(!(event.getLevel() instanceof ServerLevel level)||!RaceExperience.serverActive(level.getServer()))return;
        var helper=event.getStructureHelper();if(helper==null||!helper.resolve())return;
        var tiles=Store.get(level).tiles;
        if(helper.getToPush().stream().anyMatch(pos->tiles.containsKey(pos.asLong()))
                ||helper.getToDestroy().stream().anyMatch(pos->tiles.containsKey(pos.asLong())))event.setCanceled(true);
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event){ACTIVE.clear();}

    private static final class Store extends SavedData {
        private final Map<Long,Tile> tiles=new java.util.LinkedHashMap<>();
        static Store get(ServerLevel level){return level.getDataStorage().computeIfAbsent(Store::load,Store::new,"jem_sculk_temporary_ground");}
        static Store load(CompoundTag root){
            Store result=new Store();
            for(var tag:root.getList("Tiles",10)){
                CompoundTag row=(CompoundTag)tag;
                result.tiles.put(row.getLong("Pos"),new Tile(row.getUUID("Owner"),row.hasUUID("Domain")?row.getUUID("Domain"):new UUID(0,0),row.getLong("Until"),
                        NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),row.getCompound("Original")),row.getBoolean("Placed")));
            }
            return result;
        }
        @Override public CompoundTag save(CompoundTag root){
            ListTag rows=new ListTag();
            tiles.forEach((pos,tile)->{
                CompoundTag row=new CompoundTag();row.putLong("Pos",pos);row.putUUID("Owner",tile.owner);row.putUUID("Domain",tile.domain);row.putLong("Until",tile.until);
                row.put("Original",NbtUtils.writeBlockState(tile.original));row.putBoolean("Placed",tile.placed);rows.add(row);
            });root.put("Tiles",rows);return root;
        }
        private void restore(ServerLevel level,BlockPos position,Tile tile){
            // Never overwrite an unrelated later edit. No drops, XP or sculk spreading.
            if(tile.placed&&level.getBlockState(position).is(Blocks.SCULK))level.setBlock(position,tile.original,3);
        }
        void restoreOwner(ServerLevel level,UUID owner){
            var iterator=tiles.entrySet().iterator();
            while(iterator.hasNext()){
                var entry=iterator.next();BlockPos position=BlockPos.of(entry.getKey());
                if(!entry.getValue().owner.equals(owner)||!level.hasChunkAt(position))continue;
                restore(level,position,entry.getValue());iterator.remove();setDirty();
            }
        }
        void tick(ServerLevel level){
            // Rotate even unloaded entries: bounded inspections and no starvation behind unloaded chunks.
            int count=Math.min(128,tiles.size());
            for(int index=0;index<count;index++){
                var iterator=tiles.entrySet().iterator();var entry=iterator.next();
                long key=entry.getKey();Tile tile=entry.getValue();iterator.remove();
                BlockPos pos=BlockPos.of(key);
                if(!level.hasChunkAt(pos)){tiles.put(key,tile);continue;}
                Domain domain=ACTIVE.get(tile.owner);
                if(domain==null||domain.level!=level||!domain.id.equals(tile.domain)||tile.until<=level.getGameTime()){
                    restore(level,pos,tile);setDirty();
                }else if(!tile.placed){
                    if(!level.getBlockState(pos).equals(tile.original)){setDirty();continue;}
                    tiles.put(key,new Tile(tile.owner,tile.domain,tile.until,tile.original,true));setDirty();
                    level.setBlock(pos,Blocks.SCULK.defaultBlockState(),3);
                }else tiles.put(key,tile);
            }
        }
    }
}
