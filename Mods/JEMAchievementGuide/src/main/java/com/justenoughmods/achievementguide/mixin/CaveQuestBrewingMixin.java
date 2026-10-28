package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BrewingStandBlockEntity.class)
public abstract class CaveQuestBrewingMixin {
    @Unique private static final ThreadLocal<CaveQuestStation.Before[]> jem$caveBrew=new ThreadLocal<>();
    @Inject(method="doBrew",at=@At("HEAD"))private static void jem$before(Level world,BlockPos pos,NonNullList<ItemStack> items,CallbackInfo ci){
        jem$caveBrew.remove();if(world.isClientSide || !(world.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand))return;
        var before=new CaveQuestStation.Before[4];for(int i=0;i<4;i++)before[i]=CaveQuestStation.before(stand,i);jem$caveBrew.set(before);
    }
    @Inject(method="doBrew",at=@At("RETURN"))private static void jem$after(Level world,BlockPos pos,NonNullList<ItemStack> items,CallbackInfo ci){
        var before=jem$caveBrew.get();jem$caveBrew.remove();if(before==null || !(world.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)
                || !CaveQuestProof.item(before[3].item(),"ferrouslime_ball") || before[3].item().getCount()-items.get(3).getCount()!=1)return;
        var actor=CaveQuestStation.actor(stand,before[3]);
        for(int i=0;i<3;i++)if(PotionUtils.getPotion(items.get(i))==ACEffectRegistry.MAGNETIZING_POTION.get())CaveQuestStation.transformed(stand,i,before[i],actor,"v3_cave_metal_slime_brew");
        CaveQuestStation.consumed(stand,3,before[3]);
    }
}
