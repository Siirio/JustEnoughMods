package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reuse native per-entity ticks rather than allocate unbounded AABB herd query lists. */
@Mod.EventBusSubscriber(modid=JemSettlements.MOD_ID,value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class AnimalObservationService {
    private static int tick=-1,used;
    private static final String LAST="JemAnimalObservationTick";
    private AnimalObservationService() { }
    public static boolean supported(net.minecraft.world.entity.Mob mob) {
        return mob instanceof Animal || mob instanceof net.minecraft.world.entity.animal.WaterAnimal;
    }
    @SubscribeEvent public static void observe(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof net.minecraft.world.entity.Mob animal)||!supported(animal)||!(animal.level() instanceof ServerLevel level)||!SettlementAuthority.active(level.getServer()))return;
        int now=level.getServer().getTickCount();if(tick!=now){tick=now;used=0;}if(used>=64)return;
        var nbt=animal.getPersistentData();
        long last=nbt.getLong(LAST);if(last>0&&now>=last&&now-last<200)return;
        used++;nbt.putLong(LAST,now);
        String id=animal.getUUID().toString();var data=SettlementSavedData.get(level.getServer());
        for(var town:data.towns()) {
            var observations=town.animalObservations();
            boolean inside=town.dimension().equals(level.dimension().location())&&town.contains(animal.blockPosition());
            boolean peaceful=animal.isAlive()&&animal.getTarget()==null&&!(animal instanceof net.minecraft.world.entity.monster.Enemy)
                    &&!(animal instanceof net.minecraft.world.entity.NeutralMob neutral&&neutral.isAngry());
            if(!inside||!peaceful){observations.remove(id);continue;}
            if(observations.size()>=512&&!observations.contains(id))continue;
            var row=new CompoundTag();row.putString("Type",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString());
            row.putLong("Position",animal.blockPosition().asLong());observations.put(id,row);
        }
        data.changed();
    }
}
