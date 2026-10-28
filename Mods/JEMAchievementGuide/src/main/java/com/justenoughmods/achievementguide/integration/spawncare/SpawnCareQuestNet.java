package com.justenoughmods.achievementguide.integration.spawncare;
import java.util.*;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.InteractionHand;import net.minecraft.world.entity.Mob;import net.minecraft.world.item.ItemStack;
public final class SpawnCareQuestNet {
 public static final class Run {public final ServerPlayer actor;public final InteractionHand hand;public final List<SpawnCareQuestContainers.Captured> captures=new ArrayList<>();public Run(ServerPlayer actor,InteractionHand hand){this.actor=actor;this.hand=hand;}}
 private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();
 public static void begin(ServerPlayer actor,InteractionHand hand){CURRENT.remove();if(actor!=null)CURRENT.set(new Run(actor,hand));}
 public static void discard(Mob mob){var run=CURRENT.get();var before=run==null?null:SpawnCareQuestContainers.before(run.actor,run.hand,mob);mob.discard();if(run!=null&&before!=null&&mob.isRemoved()&&run.captures.size()<16)run.captures.add(SpawnCareQuestContainers.entry(before));}
 public static void finish(boolean accepted){var run=CURRENT.get();CURRENT.remove();if(run==null||!accepted||run.captures.isEmpty())return;ItemStack actual=run.actor.getItemInHand(run.hand);if(!actual.hasTag())return;var list=actual.getTag().getList("CapturedMobs",10);if(list.size()!=run.captures.size())return;for(int i=0;i<list.size();i++)if(!list.getCompound(i).equals(run.captures.get(i).payload()))return;SpawnCareQuestContainers.certify(run.actor,actual,run.captures,-1);}
}
