package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.SpawnCareQuestContainers;
import java.util.ArrayDeque;import java.util.Optional;
import net.minecraft.world.entity.Entity;import net.minecraft.world.entity.Mob;import net.minecraft.world.entity.player.Player;import net.minecraft.world.InteractionHand;import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Concrete native interaction envelope for Bucketable and ordinary Catchable captures. */
@Mixin(Player.class) public abstract class SpawnCareQuestBucketCaptureMixin {
 @Unique private final ArrayDeque<Optional<SpawnCareQuestContainers.InteractionCapture>> jem$captures=new ArrayDeque<>();@Unique private int jem$captureOverflow;
 @Inject(method="interactOn",at=@At("HEAD")) private void jem$before(Entity entity,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){if(jem$captureOverflow>0||jem$captures.size()>=16){jem$captureOverflow++;return;}jem$captures.push(Optional.ofNullable(entity instanceof Mob mob?SpawnCareQuestContainers.interactionBefore((Player)(Object)this,hand,mob):null));}
 @Inject(method="interactOn",at=@At("RETURN")) private void jem$after(Entity entity,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){if(jem$captureOverflow>0){jem$captureOverflow--;return;}var run=jem$captures.poll();if(run!=null)SpawnCareQuestContainers.interactionCaught(run.orElse(null),ci.getReturnValue().consumesAction());}
}
