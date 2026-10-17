package com.siirio.jemsettlements.sandbox;

import com.siirio.jemsettlements.compat.recruits.RecruitsAdapter;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/** Optional class boundary. Commands call inspected Recruits 1.15.2 setters;
 * native navigation, combat, ownership and the recruit's own NBT remain native. */
final class SandboxRecruitOrders {
    static java.util.function.BiPredicate<ServerPlayer,String> permission = (player,node) -> false;
    private static List<AbstractRecruitEntity> available(ServerPlayer player, TownRecord town) {
        List<AbstractRecruitEntity> result = new ArrayList<>();
        for (var record : SettlementSavedData.get(player.getServer()).workforce(town.id())) {
            if (!record.settlementActive() || !record.ownerId().equals(town.owner()) || SandboxCivicRuntime.get(player.getServer()).guardPending(record.entityId())) continue;
            var entity = player.serverLevel().getEntity(record.entityId());
            if (entity instanceof AbstractRecruitEntity recruit && recruit.isAlive() && player.distanceToSqr(recruit) <= 64*64
                    && RecruitsAdapter.ownedBy(recruit,town.owner()) && !recruit.getFleeing()) result.add(recruit);
        }
        result.sort(java.util.Comparator.comparingDouble(player::distanceToSqr)); return result;
    }
    static void rows(ServerPlayer player, TownRecord town, ListTag rows) {
        for (var recruit : available(player,town)) {
            if (rows.size()>=48) break;
            CompoundTag row = SandboxCivicRuntime.row(recruit.getUUID(),recruit.getName().getString(),"Свой боец · "+Math.round(recruit.getHealth())+" HP"); row.putString("Town",town.id().toString()); rows.add(row);
        }
    }
    private static AbstractRecruitEntity selected(ServerPlayer player, TownRecord town, String id) {
        return available(player,town).stream().filter(r -> r.getUUID().toString().equals(id)).findFirst().orElse(null);
    }
    private static AbstractRecruitEntity ownedLoaded(ServerPlayer player,TownRecord town,UUID id) {
        var record=SettlementSavedData.get(player.getServer()).workforceEntity(id).orElse(null);
        var entity=player.serverLevel().getEntity(id);
        return record!=null&&record.townId().equals(town.id())&&record.settlementActive()&&record.ownerId().equals(town.owner())
                &&entity instanceof AbstractRecruitEntity recruit&&recruit.isAlive()&&RecruitsAdapter.ownedBy(recruit,town.owner())?recruit:null;
    }
    private static boolean safe(ServerPlayer player, AbstractRecruitEntity recruit, TownRecord town, BlockPos pos) {
        var level = player.serverLevel();
        return town.contains(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player,pos)
                && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)
                && level.noCollision(recruit,recruit.getBoundingBox().move(pos.getX()+.5-recruit.getX(),pos.getY()-recruit.getY(),pos.getZ()+.5-recruit.getZ()));
    }
    private static BlockPos target(ServerPlayer player) {
        var hit = player.pick(48,0,false);
        return hit instanceof BlockHitResult block && hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK ? block.getBlockPos().relative(block.getDirection()) : null;
    }
    static boolean action(ServerPlayer player, TownRecord town, String node, CompoundTag input, CompoundTag metadata) {
        String mode=input.getString("Mode"); ListTag roster=metadata.getList("Guards",Tag.TAG_COMPOUND);
        if (mode.equals("select")) {
            var recruit=selected(player,town,input.getString("Id")); if(recruit==null)return false;
            for(int i=0;i<roster.size();i++) if(roster.getCompound(i).getUUID("Id").equals(recruit.getUUID())) {
                queue(player,town,roster.getCompound(i),metadata);roster.remove(i);metadata.put("Guards",roster);
                SandboxCivicRuntime.get(player.getServer()).persist(player.getServer());SandboxCivicRuntime.get(player.getServer()).restoreGuards(player.getServer());return true;
            }
            if(roster.size()>=12)return false;
            CompoundTag guard=new CompoundTag();guard.putInt("Version",2);guard.putUUID("Id",recruit.getUUID());
            roster.add(guard);metadata.put("Guards",roster);SandboxCivicRuntime.get(player.getServer()).setDirty();return true;
        }
        if(mode.equals("clear")) { stop(player,town,metadata); metadata.remove("Guards");metadata.remove("Route");SandboxCivicRuntime.get(player.getServer()).setDirty();return true; }
        if(node.equals("patrol_route") && mode.equals("add_point")) {
            BlockPos point=target(player);var recruits=available(player,town);if(point==null || recruits.isEmpty() || recruits.stream().anyMatch(r->!safe(player,r,town,point)))return false;
            ListTag route=metadata.getList("Route",Tag.TAG_COMPOUND);if(route.size()>=6)return false;
            CompoundTag row=new CompoundTag();row.putLong("Position",point.asLong());route.add(row);metadata.put("Route",route);SandboxCivicRuntime.get(player.getServer()).setDirty();return true;
        }
        if(roster.isEmpty())return false;
        for(int i=0;i<roster.size();i++)if(SandboxCivicRuntime.get(player.getServer()).guardPending(roster.getCompound(i).getUUID("Id")))return false;
        if(node.equals("captain_order") && mode.equals("captain")) {
            var captain=selected(player,town,input.getString("Id"));if(captain==null)return false;
            metadata.putUUID("Captain",captain.getUUID());metadata.putString("Order","captain_order");
            for(int i=0;i<roster.size();i++) {
                var recruit=selected(player,town,roster.getCompound(i).getUUID("Id").toString());
                if(recruit==null||recruit==captain)continue;
                prepare(roster.getCompound(i),recruit);
                recruit.setProtectUUID(java.util.Optional.of(captain.getUUID()));recruit.setFollowState(5);
                remember(roster.getCompound(i),recruit);
            }
            SandboxCivicRuntime.get(player.getServer()).setDirty();return true;
        }
        if(node.equals("rally") && mode.equals("rally")) {
            BlockPos point=target(player);if(point==null)return false;
            List<AbstractRecruitEntity> chosen=new ArrayList<>();
            for(int i=0;i<roster.size();i++){var recruit=selected(player,town,roster.getCompound(i).getUUID("Id").toString());if(recruit!=null){if(!safe(player,recruit,town,point))return false;chosen.add(recruit);}}
            if(chosen.isEmpty())return false;
            metadata.putString("Order","rally");
            for(int i=0;i<roster.size();i++){var guard=roster.getCompound(i);var recruit=selected(player,town,guard.getUUID("Id").toString());if(recruit!=null){prepare(guard,recruit);move(recruit,point);remember(guard,recruit);}}
            metadata.putLong("LastPoint",point.asLong());
            SandboxCivicRuntime.get(player.getServer()).setDirty();return true;
        }
        if(node.equals("patrol_route") && mode.equals("start")) {
            if(metadata.getList("Route",Tag.TAG_COMPOUND).isEmpty())return false;
            metadata.putString("Order","patrol_route");metadata.putInt("RouteIndex",0);metadata.remove("LastPoint");SandboxCivicRuntime.get(player.getServer()).setDirty();tick(player,town,metadata);return true;
        }
        return false;
    }
    private static void move(AbstractRecruitEntity recruit,BlockPos point){recruit.setFollowState(0);recruit.setMovePos(point);recruit.setShouldMovePos(true);}
    private static CompoundTag nativeOrder(AbstractRecruitEntity recruit){
        CompoundTag row=new CompoundTag();row.putInt("Follow",recruit.getFollowState());
        row.putBoolean("Move",recruit.getShouldMovePos());row.putBoolean("Hold",recruit.getShouldHoldPos());
        row.putBoolean("Protect",recruit.getShouldProtect());row.putBoolean("FollowOwner",recruit.getShouldFollow());
        if(recruit.getMovePos()!=null)row.putLong("Position",recruit.getMovePos().asLong());
        if(recruit.getProtectUUID()!=null)row.putUUID("ProtectId",recruit.getProtectUUID());
        var hold=recruit.getHoldPos();if(hold!=null){row.putDouble("HoldX",hold.x);row.putDouble("HoldY",hold.y);row.putDouble("HoldZ",hold.z);}
        return row;
    }
    private static boolean ours(AbstractRecruitEntity recruit,CompoundTag guard){
        if(!guard.hasUUID("Token")||!guard.getUUID("Token").toString().equals(recruit.getPersistentData().getString("JemSandboxGuardOrder")))return false;
        CompoundTag expected=guard.getCompound("Expected"),actual=nativeOrder(recruit);
        if(expected.equals(actual))return true;
        // Native MoveToPosGoal clears its destination on arrival; that completion is still ours.
        if(expected.contains("Position")&&recruit.reachedMovePos&&recruit.getMovePos()==null&&!recruit.getShouldMovePos()){
            expected=expected.copy();expected.remove("Position");expected.putBoolean("Move",false);return expected.equals(actual);
        }
        return false;
    }
    private static void prepare(CompoundTag guard,AbstractRecruitEntity recruit){
        if(!guard.contains("Before")||!ours(recruit,guard))guard.put("Before",nativeOrder(recruit));
        guard.putUUID("Token",UUID.randomUUID());
    }
    private static void remember(CompoundTag guard,AbstractRecruitEntity recruit){
        guard.put("Expected",nativeOrder(recruit));recruit.getPersistentData().putString("JemSandboxGuardOrder",guard.getUUID("Token").toString());
    }
    private static void queue(ServerPlayer player,TownRecord town,CompoundTag guard,CompoundTag metadata){
        CompoundTag pending=guard.copy();
        if(!pending.contains("Expected")||!pending.contains("Before")||!pending.hasUUID("Token"))return; // Selection alone never proves command ownership.
        pending.putUUID("Owner",town.owner());pending.putUUID("Town",town.id());pending.putString("Dimension",town.dimension().toString());
        SandboxCivicRuntime.get(player.getServer()).queueGuard(guard.getUUID("Id"),pending);
    }
    private static void stop(ServerPlayer player,TownRecord town,CompoundTag metadata){
        ListTag roster=metadata.getList("Guards",Tag.TAG_COMPOUND);
        for(int i=0;i<roster.size();i++)queue(player,town,roster.getCompound(i),metadata);
        metadata.remove("Order");metadata.remove("LastPoint");metadata.remove("Captain");
        var data=SandboxCivicRuntime.get(player.getServer());data.persist(player.getServer());data.restoreGuards(player.getServer());
        for(int i=0;i<roster.size();i++){roster.getCompound(i).remove("Before");roster.getCompound(i).remove("Expected");roster.getCompound(i).remove("Token");}
    }
    static void cancel(ServerPlayer player,TownRecord town,CompoundTag metadata){stop(player,town,metadata);}
    static boolean restore(MinecraftServer server,Entity entity,CompoundTag pending){
        if(!(entity instanceof AbstractRecruitEntity recruit))return true;
        if(!recruit.isAlive())return true;
        if(!RecruitsAdapter.ownedBy(recruit,pending.getUUID("Owner")))return true;
        var record=SettlementSavedData.get(server).workforceEntity(recruit.getUUID()).orElse(null);
        if(record!=null&&(!record.living()||!record.ownerId().equals(pending.getUUID("Owner"))||!record.townId().equals(pending.getUUID("Town"))))return true;
        if(!pending.contains("Expected")||!pending.contains("Before")||!pending.hasUUID("Token"))return false;
        if(!ours(recruit,pending))return true; // A newer native/user order wins; no mutation.
        CompoundTag before=pending.getCompound("Before");
        recruit.clearMovePos();recruit.clearHoldPos();recruit.setProtectUUID(before.hasUUID("ProtectId")?java.util.Optional.of(before.getUUID("ProtectId")):java.util.Optional.empty());
        recruit.setFollowState(before.getInt("Follow"));
        if(before.contains("HoldX"))recruit.setHoldPos(new net.minecraft.world.phys.Vec3(before.getDouble("HoldX"),before.getDouble("HoldY"),before.getDouble("HoldZ")));
        if(before.contains("Position"))recruit.setMovePos(BlockPos.of(before.getLong("Position")));
        recruit.setShouldMovePos(before.getBoolean("Move"));recruit.setShouldHoldPos(before.getBoolean("Hold"));recruit.setShouldProtect(before.getBoolean("Protect"));recruit.setShouldFollow(before.getBoolean("FollowOwner"));
        recruit.getPersistentData().remove("JemSandboxGuardOrder");return true;
    }
    static void tick(ServerPlayer player,TownRecord town,CompoundTag metadata){
        String order=metadata.getString("Order");if(order.isEmpty())return;
        if(!permission.test(player,order)){stop(player,town,metadata);return;}
        if(order.equals("captain_order")){
            var captain=metadata.hasUUID("Captain")?ownedLoaded(player,town,metadata.getUUID("Captain")):null;
            if(captain==null)stop(player,town,metadata);return;
        }
        if(!order.equals("patrol_route"))return;
        ListTag route=metadata.getList("Route",Tag.TAG_COMPOUND),roster=metadata.getList("Guards",Tag.TAG_COMPOUND);if(route.isEmpty()||roster.isEmpty())return;
        boolean arrived=true;List<AbstractRecruitEntity> recruits=new ArrayList<>();
        for(int i=0;i<roster.size();i++){
            var recruit=ownedLoaded(player,town,roster.getCompound(i).getUUID("Id"));if(recruit==null)continue;
            var guard=roster.getCompound(i);
            if(guard.contains("Expected")&&!ours(recruit,guard))continue;
            if(metadata.contains("LastPoint")){
                BlockPos last=BlockPos.of(metadata.getLong("LastPoint"));
                if(recruit.distanceToSqr(last.getX()+.5,last.getY(),last.getZ()+.5)>9)arrived=false;
            }
            recruits.add(recruit);
        }
        if(!arrived||recruits.isEmpty())return;
        int index=Math.floorMod(metadata.getInt("RouteIndex"),route.size());BlockPos next=BlockPos.of(route.getCompound(index).getLong("Position"));
        if(recruits.stream().anyMatch(r->!safe(player,r,town,next)))return;
        for(var recruit:recruits)for(int i=0;i<roster.size();i++)if(roster.getCompound(i).getUUID("Id").equals(recruit.getUUID())){
            var guard=roster.getCompound(i);prepare(guard,recruit);move(recruit,next);remember(guard,recruit);break;
        }
        metadata.putLong("LastPoint",next.asLong());metadata.putInt("RouteIndex",(index+1)%route.size());
    }
}
