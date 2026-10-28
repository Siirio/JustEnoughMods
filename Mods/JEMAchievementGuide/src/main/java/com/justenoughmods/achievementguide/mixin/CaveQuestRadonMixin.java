package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.GeothermalVentBlock.class,remap=false)
public abstract class CaveQuestRadonMixin {
    @Unique private static final ThreadLocal<int[]> jem$caveCapture=new ThreadLocal<>();
    @Inject(method="m_6227_",at=@At("HEAD"))private void jem$before(BlockState state,Level level,BlockPos pos,Player actor,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        var bottle=new ItemStack(com.github.alexmodguy.alexscaves.server.item.ACItemRegistry.RADON_BOTTLE.get());
        jem$caveCapture.set(new int[]{actor.getItemInHand(hand).is(net.minecraft.world.item.Items.GLASS_BOTTLE)?actor.getItemInHand(hand).getCount():0,CaveQuestProof.count(actor.getInventory(),bottle)});
    }
    @Inject(method="m_6227_",at=@At("RETURN"))private void jem$after(BlockState state,Level level,BlockPos pos,Player user,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        int[] before=jem$caveCapture.get();jem$caveCapture.remove();var actor=CaveQuestProof.actor(user);
        if(before!=null && before[0]>0 && before[0]-(user.getItemInHand(hand).is(net.minecraft.world.item.Items.GLASS_BOTTLE)?user.getItemInHand(hand).getCount():0)==1 && CaveQuestProof.hazmat(actor)
                && CaveQuestProof.count(actor.getInventory(),new ItemStack(com.github.alexmodguy.alexscaves.server.item.ACItemRegistry.RADON_BOTTLE.get()))-before[1]==1)CaveQuestProof.emit(actor,"v3_cave_hazmat_processing_radon");
    }
}
