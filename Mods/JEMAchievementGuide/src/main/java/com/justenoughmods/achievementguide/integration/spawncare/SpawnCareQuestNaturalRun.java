package com.justenoughmods.achievementguide.integration.spawncare;
import java.util.*;import net.minecraft.world.entity.Entity;import net.minecraft.world.entity.item.ItemEntity;
public final class SpawnCareQuestNaturalRun {
 public final Entity source;public final List<ItemEntity> drops=new ArrayList<>();private static final ThreadLocal<ArrayDeque<SpawnCareQuestNaturalRun>> CURRENT=ThreadLocal.withInitial(ArrayDeque::new);
 private SpawnCareQuestNaturalRun(Entity source){this.source=source;}
 public static SpawnCareQuestNaturalRun begin(Entity source){var run=new SpawnCareQuestNaturalRun(source);CURRENT.get().push(run);return run;}
 public static void inserted(Entity entity,boolean accepted){if(accepted&&entity instanceof ItemEntity drop)for(var run:CURRENT.get())if(drop.level()==run.source.level()&&run.drops.size()<32)run.drops.add(drop);}
 public static void end(SpawnCareQuestNaturalRun run){if(run==null)return;var queue=CURRENT.get();queue.removeFirstOccurrence(run);if(queue.isEmpty())CURRENT.remove();for(var drop:run.drops)if(SpawnCareQuestProof.item(drop.getItem()).equals("spawn:music_disc_water_horse"))SpawnCareQuestDrops.mark(drop,run.source,"v3_pet_sea_sound");}
}