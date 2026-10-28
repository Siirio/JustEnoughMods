package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraft.world.entity.*;import net.minecraft.world.phys.Vec3;import java.util.ArrayDeque;import java.util.Optional;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Entity.class,priority=500) public abstract class WorldBattleArmorQuestMoveMixin {
 @Unique private ArrayDeque<Optional<WorldBattleArmorQuestMotion.Move>> jem$armorMoves;
 @Inject(method="move",at=@At("HEAD")) private void jem$begin(MoverType type,Vec3 delta,CallbackInfo ci){if((Object)this instanceof net.minecraft.server.level.ServerPlayer){if(jem$armorMoves==null)jem$armorMoves=new ArrayDeque<>();jem$armorMoves.push(Optional.ofNullable(WorldBattleArmorQuestMotion.begin((Entity)(Object)this)));}}
 @Inject(method="move",at=@At("RETURN")) private void jem$end(MoverType type,Vec3 delta,CallbackInfo ci){if(jem$armorMoves!=null&&!jem$armorMoves.isEmpty())WorldBattleArmorQuestMotion.end(jem$armorMoves.pop().orElse(null));}
}
