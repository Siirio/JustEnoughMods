package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=com.github.alexmodguy.alexscaves.server.item.MarineSnowItem.class,remap=false)
public abstract class CaveQuestMarineSnowMixin {
    @Shadow private static Map<Block,Block> GROWS_INTERACTIONS;
    @Unique private static final ThreadLocal<CompoundTag> jem$caveBefore=new ThreadLocal<>();
    @Unique private static final ThreadLocal<BlockState> jem$caveCoral=new ThreadLocal<>();
    @Inject(method="m_6225_",at=@At("HEAD"))private void jem$before(UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){jem$caveBefore.set(CaveQuestStackProof.get(context.getItemInHand()));jem$caveCoral.set(context.getLevel().getBlockState(context.getClickedPos()));}
    @Inject(method="m_6225_",at=@At("RETURN"))private void jem$after(UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){
        var proof=jem$caveBefore.get();var before=jem$caveCoral.get();jem$caveBefore.remove();jem$caveCoral.remove();var actor=CaveQuestProof.actor(context.getPlayer());
        if(actor==null || proof==null || proof.isEmpty() || !proof.getString("kind").equals("marine") || !proof.getUUID("actor").equals(actor.getUUID())
                || proof.getInt("count")-context.getItemInHand().getCount()!=1)return;
        if(GROWS_INTERACTIONS!=null && before!=null && GROWS_INTERACTIONS.get(before.getBlock())==context.getLevel().getBlockState(context.getClickedPos()).getBlock()
                && before.getBlock()!=context.getLevel().getBlockState(context.getClickedPos()).getBlock())CaveQuestProof.emit(actor,"v3_cave_marine_revival");
        CaveQuestStackProof.put(context.getItemInHand(),actor.getUUID(),proof.getUUID("source"),"marine");
    }
}
