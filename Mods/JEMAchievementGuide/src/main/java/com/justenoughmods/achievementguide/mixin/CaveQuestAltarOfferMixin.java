package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.block.blockentity.AbyssalAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.AbyssalAltarBlock.class,remap=false)
public abstract class CaveQuestAltarOfferMixin {
    @Unique private static final ThreadLocal<Integer> jem$caveOffer=new ThreadLocal<>();
    @Inject(method="m_6227_",at=@At("HEAD"))private void jem$before(BlockState state,Level level,BlockPos pos,Player actor,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){jem$caveOffer.set(actor.getItemInHand(hand).getCount());}
    @Inject(method="m_6227_",at=@At("RETURN"))private void jem$after(BlockState state,Level level,BlockPos pos,Player user,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        Integer count=jem$caveOffer.get();jem$caveOffer.remove();var actor=CaveQuestProof.actor(user);
        if(count!=null && count-user.getItemInHand(hand).getCount()==1 && level.getBlockEntity(pos) instanceof AbyssalAltarBlockEntity altar){
            altar.getPersistentData().remove("jemCaveOfferingV4");
            if(actor!=null && altar.getItem(0).is(com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry.DEEP_ONE_BARTERS)){
                var proof=new net.minecraft.nbt.CompoundTag();proof.putUUID("actor",actor.getUUID());proof.put("item",altar.getItem(0).save(new net.minecraft.nbt.CompoundTag()));altar.getPersistentData().put("jemCaveOfferingV4",proof);altar.setChanged();}
        }
    }
}
