package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.SpawnCareQuestContainers;
import java.util.ArrayDeque;import java.util.Optional;
import net.minecraft.world.entity.Mob;import net.minecraft.world.entity.player.Player;import net.minecraft.world.InteractionHand;import net.minecraft.world.InteractionResult;import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Octopus owns capture in interactAt, outside Player.interactOn. */
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.mob.Octopus",remap=false) public abstract class SpawnCareQuestCatchableMixin {
 @Unique private final ArrayDeque<Optional<SpawnCareQuestContainers.InteractionCapture>> jem$captures=new ArrayDeque<>();@Unique private int jem$captureOverflow;
 @Inject(method="m_7111_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",at=@At("HEAD"),remap=false) private void jem$before(Player player,Vec3 point,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){if(jem$captureOverflow>0||jem$captures.size()>=16){jem$captureOverflow++;return;}jem$captures.push(Optional.ofNullable(SpawnCareQuestContainers.interactionBefore(player,hand,(Mob)(Object)this)));}
 @Inject(method="m_7111_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",at=@At("RETURN"),remap=false) private void jem$after(Player player,Vec3 point,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){if(jem$captureOverflow>0){jem$captureOverflow--;return;}var run=jem$captures.poll();if(run!=null)SpawnCareQuestContainers.interactionCaught(run.orElse(null),ci.getReturnValue().consumesAction());}
}
