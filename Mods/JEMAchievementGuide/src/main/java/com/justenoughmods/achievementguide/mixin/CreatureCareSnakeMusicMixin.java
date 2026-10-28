package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareMusic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.primal.entity.animal.SnakeEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.SnakeEntity$SongListener", remap = false)
public abstract class CreatureCareSnakeMusicMixin {
    @Shadow @Final private SnakeEntity snake;
    @Inject(method = "m_214068_", at = @At("RETURN"))
    private void jem$music(ServerLevel level, GameEvent event, GameEvent.Context context, Vec3 pos, CallbackInfoReturnable<Boolean> callback) {
        CreatureCareMusic.accepted(snake, level, event, pos, callback.getReturnValue());
    }
}
