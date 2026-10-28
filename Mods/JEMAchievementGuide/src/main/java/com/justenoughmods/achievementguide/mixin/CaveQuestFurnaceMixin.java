package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class CaveQuestFurnaceMixin {
    @Unique private CaveQuestStation.Before jem$caveInput,jem$caveOutput;
    @Inject(method="burn",at=@At("HEAD"))private void jem$before(RegistryAccess registry,Recipe<?> recipe,NonNullList<ItemStack> items,int max,CallbackInfoReturnable<Boolean> ci){var owner=(AbstractFurnaceBlockEntity)(Object)this;jem$caveInput=CaveQuestStation.before(owner,0);jem$caveOutput=CaveQuestStation.before(owner,2);}
    @Inject(method="burn",at=@At("RETURN"))private void jem$after(RegistryAccess registry,Recipe<?> recipe,NonNullList<ItemStack> items,int max,CallbackInfoReturnable<Boolean> ci){
        var owner=(AbstractFurnaceBlockEntity)(Object)this;
        if(ci.getReturnValue() && recipe!=null && jem$caveInput!=null && jem$caveInput.item().getCount()-items.get(0).getCount()==1
                && CaveQuestProof.item(jem$caveInput.item(),"metal_swarf") && CaveQuestProof.item(items.get(2),"scrap_metal")){
            var actor=CaveQuestStation.actor(owner,jem$caveInput);CaveQuestStation.output(owner,2,jem$caveOutput,actor,"v3_cave_metal_slime");CaveQuestStation.consumed(owner,0,jem$caveInput);
        }jem$caveInput=null;jem$caveOutput=null;
    }
}
