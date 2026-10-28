package com.justenoughmods.achievementguide.mixin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="com.yungnickyoung.minecraft.ribbits.player.PlayerInstrumentTracker",remap=false)
public interface PetUtilityQuestInstrumentAccess {
 @Accessor("performerToAudienceMap")static ConcurrentHashMap<Player,Set<Player>> jem$petPerformers(){throw new AssertionError();}
}
