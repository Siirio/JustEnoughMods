package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;import net.minecraft.world.entity.Mob;import net.minecraft.world.entity.player.Player;import net.minecraft.world.level.Level;import net.minecraft.world.*;import net.minecraft.world.item.ItemStack;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.ninni.spawn.server.item.CastingNetItem",remap=false) public abstract class SpawnCareQuestNetCaptureMixin {
 @Inject(method="m_7203_",at=@At("HEAD"),remap=false) private void jem$before(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){SpawnCareQuestNet.begin(SpawnCareQuestProof.real(player),hand);}
 @Redirect(method="m_7203_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Mob;m_146870_()V",remap=false),remap=false) private void jem$discard(Mob mob){SpawnCareQuestNet.discard(mob);}
 @Inject(method="m_7203_",at=@At("RETURN"),remap=false) private void jem$after(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){SpawnCareQuestNet.finish(ci.getReturnValue().getResult().consumesAction());}
}
