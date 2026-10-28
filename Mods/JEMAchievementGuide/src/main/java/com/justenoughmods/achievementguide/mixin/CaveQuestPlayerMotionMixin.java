package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(ServerPlayer.class)
public abstract class CaveQuestPlayerMotionMixin implements CaveQuestMotionCarrier {
    @Unique private final CaveQuestMotion jem$caveMotion=new CaveQuestMotion();
    @Override public CaveQuestMotion jem$caveMotion(){return jem$caveMotion;}
}
