package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.item.RemoteDetonatorItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=RemoteDetonatorItem.class,remap=false)
public abstract class CaveQuestDetonatorMixin {
    @Inject(method="m_6225_",at=@At("RETURN"))private void jem$actualBinding(UseOnContext context,CallbackInfoReturnable<net.minecraft.world.InteractionResult> ci){
        var actor=CaveQuestProof.actor(context.getPlayer());var stack=context.getItemInHand();var bound=stack.hasTag()?RemoteDetonatorItem.getBombPosition(stack.getTag()):null;
        if(actor!=null && ci.getReturnValue().consumesAction() && bound!=null && bound.dimension().equals(actor.level().dimension()) && bound.pos().equals(context.getClickedPos()) && CaveQuestProof.permitted(actor,bound.pos()))CaveQuestStackProof.put(stack,actor.getUUID(),java.util.UUID.randomUUID(),"detonator");
    }
    @Inject(method="m_7203_",at=@At("HEAD"))private void jem$before(Level world,Player user,InteractionHand hand,CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<ItemStack>> ci){CaveQuestDetonation.begin(CaveQuestProof.actor(user),user.getItemInHand(hand));}
    @Redirect(method="m_7203_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;onCaughtFire(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void jem$actualNativeFire(net.minecraft.world.level.block.state.BlockState state,Level world,net.minecraft.core.BlockPos pos,net.minecraft.core.Direction side,net.minecraft.world.entity.LivingEntity user){CaveQuestDetonation.fire(state,world,pos,side,user);}
    @Inject(method="m_7203_",at=@At("RETURN"))private void jem$after(Level world,Player user,InteractionHand hand,CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<ItemStack>> ci){CaveQuestDetonation.end();}
}
