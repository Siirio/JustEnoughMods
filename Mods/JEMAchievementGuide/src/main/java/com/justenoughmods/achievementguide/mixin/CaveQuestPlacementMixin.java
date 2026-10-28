package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestBlockOrigins;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestPlacementRun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public abstract class CaveQuestPlacementMixin {
    @Shadow public abstract Block getBlock();
    
    @Inject(method="place",at=@At("HEAD"))
    private void jem$caveBefore(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci){ CaveQuestPlacementRun.begin(context.getPlayer() instanceof ServerPlayer actor?actor:null,context.getClickedPos(),getBlock(),context.getItemInHand().getCount()); }
    @Inject(method="place",at=@At("RETURN"))
    private void jem$cavePlaced(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci){
        var run=CaveQuestPlacementRun.end(); Integer before=run==null?null:run.count;
        if(before==null || !ci.getReturnValue().consumesAction() || !(context.getPlayer() instanceof ServerPlayer actor)
                || CaveQuestProof.actor(actor)==null || before-context.getItemInHand().getCount()!=1) return;
        var pos=context.getClickedPos(); var state=actor.serverLevel().getBlockState(pos);
        if(run.rust!=null && state.equals(run.rust) && CaveQuestProof.hazmat(actor)) CaveQuestProof.emit(actor,"v3_cave_hazmat_processing");
        if(state.getBlock()!=getBlock()) return;
        String id=String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
        if(!id.startsWith("alexscaves:") && state.getLightEmission(actor.serverLevel(),pos)<=7) return;
        CompoundTag origin=new CompoundTag();origin.putUUID("actor",actor.getUUID());origin.putString("block",id);
        CaveQuestBlockOrigins.put(actor.serverLevel(),pos,"Placed",origin);
        var owner=actor.serverLevel().getBlockEntity(pos);
        if(owner!=null && id.startsWith("alexscaves:")) CaveQuestProof.configured(owner,actor);
    }
}
