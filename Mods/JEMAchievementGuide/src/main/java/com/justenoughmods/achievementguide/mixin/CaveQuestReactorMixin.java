package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=NuclearFurnaceBlockEntity.class,remap=false)
public abstract class CaveQuestReactorMixin implements CaveQuestReactorReceipt {
    @Shadow private AbstractCookingRecipe currentRecipe;
    @Shadow private int currentWaste;
    @Unique private CaveQuestStation.Before[] jem$caveBefore;
    @Unique private AbstractCookingRecipe jem$caveRecipe;
    @Unique private int jem$caveWaste;
    @Inject(method="tick",at=@At("HEAD"))private static void jem$before(Level level,BlockPos pos,BlockState state,NuclearFurnaceBlockEntity reactor,CallbackInfo ci){((CaveQuestReactorReceipt)(Object)reactor).jem$caveReactorBefore();}
    @Inject(method="tick",at=@At("RETURN"))private static void jem$after(Level level,BlockPos pos,BlockState state,NuclearFurnaceBlockEntity reactor,CallbackInfo ci){((CaveQuestReactorReceipt)(Object)reactor).jem$caveReactorAfter();}
    @Override public void jem$caveReactorBefore(){
        var reactor=(NuclearFurnaceBlockEntity)(Object)this;jem$caveRecipe=currentRecipe;jem$caveWaste=currentWaste;
        jem$caveBefore=new CaveQuestStation.Before[5];for(int i=0;i<5;i++)jem$caveBefore[i]=CaveQuestStation.before(reactor,i);
    }
    @Override public void jem$caveReactorAfter(){
        var reactor=(NuclearFurnaceBlockEntity)(Object)this;var level=reactor.getLevel();var before=jem$caveBefore;jem$caveBefore=null;
        if(level==null || level.isClientSide || before==null)return;
        if(jem$caveRecipe!=null && before[0].item().getCount()-reactor.getItem(0).getCount()==1 && !jem$caveRecipe.getResultItem(level.registryAccess()).isEmpty()
                && net.minecraft.world.item.ItemStack.isSameItemSameTags(jem$caveRecipe.getResultItem(level.registryAccess()),reactor.getItem(3))){
            CaveQuestStation.output(reactor,3,before[3],CaveQuestStation.actor(reactor,before[0]),"v3_cave_reactor_output");CaveQuestStation.consumed(reactor,0,before[0]);
        }
        if(before[2].item().getCount()-reactor.getItem(2).getCount()==1 && currentWaste<jem$caveWaste && CaveQuestProof.item(reactor.getItem(4),"waste_drum")){
            CaveQuestStation.output(reactor,4,before[4],CaveQuestStation.actor(reactor,before[2]),"v3_cave_reactor_output_waste");CaveQuestStation.consumed(reactor,2,before[2]);
        }
    }
}
