package com.justenoughmods.achievementguide.integration.petutilities;
import net.mehvahdjukaar.snowyspirit.common.entity.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
/** Samples only the native friction owner's actual colliding surface calls. */
public final class PetUtilityQuestSnowContact {
 public static final class Sample {public final SledEntity sled;public boolean ice;public Sample(SledEntity sled){this.sled=sled;}}
 private static final ThreadLocal<Sample> CURRENT=new ThreadLocal<>();
 private PetUtilityQuestSnowContact(){}
 public static boolean contact(SledEntity sled){var sample=new Sample(sled);CURRENT.set(sample);try{var status=GroundStatus.computeFriction(sled).getFirst();return status.onSnow() || status.touchingGround() && sample.ice;}finally{CURRENT.remove();}}
 public static void surface(Entity e,BlockState state){var sample=CURRENT.get();if(sample!=null && sample.sled==e && state.is(net.minecraft.tags.BlockTags.ICE))sample.ice=true;}
}
