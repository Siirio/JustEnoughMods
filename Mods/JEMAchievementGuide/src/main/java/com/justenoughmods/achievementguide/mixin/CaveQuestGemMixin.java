package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.block.blockentity.BeholderBlockEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.BeholderEyeEntity;
import com.github.alexmodguy.alexscaves.server.item.OccultGemItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=OccultGemItem.class,remap=false)
public abstract class CaveQuestGemMixin {
    @Redirect(method="m_7203_",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/block/blockentity/BeholderBlockEntity;startObserving(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;)V"))
    private void jem$nativeObservation(BeholderBlockEntity owner,Level world,Player user,Level original,Player originalUser,InteractionHand hand){
        var actor=CaveQuestProof.actor(user);var stack=user.getItemInHand(hand);var bound=stack.hasTag()?OccultGemItem.getBeholderPosition(stack.getTag()):null;
        owner.startObserving(world,user);
        if(actor!=null && world==actor.level() && bound!=null && bound.dimension().equals(world.dimension()) && bound.pos().equals(owner.getBlockPos())
                && owner.getUsingEntity() instanceof BeholderEyeEntity eye && actor.getUUID().equals(eye.getUsingPlayerUUID()) && actor.serverLevel().getEntity(eye.getUUID())==eye){
            eye.getPersistentData().putUUID("jemCaveGemActorV4",actor.getUUID());eye.getPersistentData().putLong("jemCaveGemBlockV4",owner.getBlockPos().asLong());}
    }
}
