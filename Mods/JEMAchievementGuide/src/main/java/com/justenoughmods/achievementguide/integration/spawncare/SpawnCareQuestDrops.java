package com.justenoughmods.achievementguide.integration.spawncare;
import java.util.UUID;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.entity.*;import net.minecraft.world.entity.item.ItemEntity;import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
public final class SpawnCareQuestDrops {
 public static final UUID NATURAL=new UUID(0L,0L);
 public static boolean nativeMob(Entity entity){return entity instanceof Mob mob&&mob.getSpawnType()!=null&&mob.getSpawnType()!=MobSpawnType.COMMAND&&mob.getSpawnType()!=MobSpawnType.SPAWN_EGG;}
 public static void mark(ItemEntity drop,Entity source,String action){if(drop!=null&&source!=null&&drop.level() instanceof ServerLevel level&&level.getEntity(drop.getUUID())==drop&&!drop.isRemoved()&&!drop.getItem().isEmpty()){if(NativeFoodSources.get(level).certify(level,NATURAL,"spawn_care_"+action,drop.getItem(),drop.getItem().getCount())!=null){var proof=SpawnCareQuestProof.data(drop);proof.putString("natural_action",action);proof.putUUID("native_source",source.getUUID());}}}
}
