package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class WorldPrehistoricQuestFurnaceMixin {
 @Unique private WorldPrehistoricQuestFurnace.Burn jem$burn;
 @Inject(method="burn",at=@At("HEAD"))private void jem$before(RegistryAccess registry,Recipe<?> recipe,NonNullList<ItemStack> items,int limit,CallbackInfoReturnable<Boolean> ci){jem$burn=WorldPrehistoricQuestFurnace.before((AbstractFurnaceBlockEntity)(Object)this,recipe);}
 @Inject(method="burn",at=@At("RETURN"))private void jem$after(RegistryAccess registry,Recipe<?> recipe,NonNullList<ItemStack> items,int limit,CallbackInfoReturnable<Boolean> ci){var frame=jem$burn;jem$burn=null;WorldPrehistoricQuestFurnace.after((AbstractFurnaceBlockEntity)(Object)this,frame,ci.getReturnValueZ());}
}
