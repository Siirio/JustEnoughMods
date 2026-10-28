package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.block.blockentity.MagnetBlockEntity;
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
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.MagnetBlock.class,remap=false)
public abstract class CaveQuestMagnetRangeMixin {
    @Unique private static final ThreadLocal<int[]> jem$caveRange=new ThreadLocal<>();
    @Inject(method="m_6227_",at=@At("HEAD"))private void jem$before(BlockState state,Level level,BlockPos pos,Player actor,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        jem$caveRange.set(new int[]{level.getBlockEntity(pos) instanceof MagnetBlockEntity magnet?magnet.getEffectiveRange():Integer.MIN_VALUE,actor.getItemInHand(hand).getCount()});
    }
    @Inject(method="m_6227_",at=@At("RETURN"))private void jem$after(BlockState state,Level level,BlockPos pos,Player user,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        int[] before=jem$caveRange.get();jem$caveRange.remove();var actor=CaveQuestProof.actor(user);
        if(actor!=null && before!=null && level.getBlockEntity(pos) instanceof MagnetBlockEntity magnet && before[0]!=Integer.MIN_VALUE
                && before[0]!=magnet.getEffectiveRange() && before[1]-actor.getItemInHand(hand).getCount()==1){CaveQuestProof.configured(magnet,actor);CaveQuestProof.emit(actor,"v3_cave_magnet_workshop_range");}
    }
}
