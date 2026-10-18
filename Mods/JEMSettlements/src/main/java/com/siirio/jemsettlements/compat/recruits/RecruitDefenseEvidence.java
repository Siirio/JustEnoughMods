package com.siirio.jemsettlements.compat.recruits;

import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;

/** Native guard orders are observed once per unique assignment; unloaded facts stay explicitly stale. */
public final class RecruitDefenseEvidence {
    private RecruitDefenseEvidence() { }
    public static int count(SettlementSavedData data,TownRecord town,ServerLevel level,CompoundTag metrics) {
        var observations=metrics.getCompound("GuardObservations").copy();
        var assigned=new java.util.HashSet<String>();boolean stale=false,unknown=false;int count=0;
        for(var record:data.workforce(town.id())) {
            String key=record.entityId().toString();assigned.add(key);
            if(!record.settlementActive()||!town.owner().equals(record.ownerId())){observations.remove(key);continue;}
            var entity=level.getEntity(record.entityId());
            if(entity instanceof AbstractRecruitEntity recruit) {
                boolean qualifies=recruit.isAlive()&&!recruit.isNoAi()&&recruit.getIsOwned()&&record.ownerId().equals(recruit.getOwnerUUID())
                        &&town.contains(recruit.blockPosition())&&recruit.getShouldHoldPos()&&!recruit.getShouldFollow();
                var hold=recruit.getHoldPos();qualifies&=hold!=null&&town.contains(net.minecraft.core.BlockPos.containing(hold));
                var row=new CompoundTag();row.putBoolean("Qualified",qualifies);row.putLong("Position",recruit.blockPosition().asLong());observations.put(key,row);
            } else {
                stale=true;
                if(!observations.contains(key))unknown=true;
                // A loaded entity in another dimension is factual departure, not an unload.
                for(var other:level.getServer().getAllLevels())if(other!=level&&other.getEntity(record.entityId())!=null)observations.remove(key);
            }
            if(observations.getCompound(key).getBoolean("Qualified"))count++;
        }
        for(String key:java.util.Set.copyOf(observations.getAllKeys()))if(!assigned.contains(key))observations.remove(key);
        metrics.put("GuardObservations",observations);metrics.putBoolean("GuardsStale",stale);metrics.putBoolean("GuardsUnknown",unknown);
        return count;
    }
}
