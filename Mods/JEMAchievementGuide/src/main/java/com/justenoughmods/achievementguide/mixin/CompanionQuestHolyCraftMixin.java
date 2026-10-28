package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.CompanionQuestHolyCraft;import com.justenoughmods.achievementguide.integration.food.NativePreparedMealProof;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.inventory.AbstractContainerMenu;import net.minecraft.world.item.ItemStack;import net.minecraft.world.item.crafting.Recipe;import java.util.List;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=NativePreparedMealProof.class,remap=false) public abstract class CompanionQuestHolyCraftMixin {
 @Inject(method="begin",at=@At("HEAD"),remap=false) private static void jem$begin(ServerPlayer actor,AbstractContainerMenu menu,CallbackInfo ci){CompanionQuestHolyCraft.begin(actor,menu);}
 @Inject(method="crafted",at=@At("HEAD"),remap=false) private static void jem$paid(ServerPlayer actor,Recipe<?> recipe,ItemStack output,List<ItemStack> removed,CallbackInfo ci){CompanionQuestHolyCraft.crafted(actor,recipe,output,removed);}
 @Inject(method="end",at=@At("RETURN"),remap=false) private static void jem$end(ServerPlayer actor,AbstractContainerMenu menu,CallbackInfo ci){CompanionQuestHolyCraft.end(actor,menu);}
}
