package com.justenoughmods.achievementguide.integration.petutilities;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
/** Only entities accepted by ServerLevel during this exact native emission call. */
public final class PetUtilityQuestDrops {
 private static final ThreadLocal<ArrayDeque<List<ItemEntity>>> RUNS=ThreadLocal.withInitial(ArrayDeque::new);
 private PetUtilityQuestDrops(){}
 public static <T> T observe(Supplier<T> operation,java.util.function.Consumer<List<ItemEntity>> committed){var rows=new ArrayList<ItemEntity>();var frames=RUNS.get();frames.push(rows);boolean success=false;try{T result=operation.get();success=true;return result;}finally{frames.pop();if(frames.isEmpty())RUNS.remove();if(success)committed.accept(List.copyOf(rows));}}
 public static void accepted(Entity entity){var run=RUNS.get().peek();if(run!=null && run.size()<128 && entity instanceof ItemEntity item && !item.getItem().isEmpty())run.add(item);}
}
