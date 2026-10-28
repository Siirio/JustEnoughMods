package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestStation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value={net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class,net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class},remap=true)
public abstract class CaveQuestStationMutationMixin {
    @Inject(method="setItem",at=@At("HEAD"))private void jem$caveChanged(int slot,ItemStack item,CallbackInfo ci){CaveQuestStation.invalidated((BlockEntity)(Object)this,slot);}
}
