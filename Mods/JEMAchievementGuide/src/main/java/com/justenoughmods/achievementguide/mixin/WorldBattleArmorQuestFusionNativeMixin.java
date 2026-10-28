package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import com.github.L_Ender.cataclysm.crafting.WeaponfusionRecipe;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;import net.minecraft.world.inventory.AbstractContainerMenu;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.github.L_Ender.cataclysm.inventory.WeaponfusionMenu",remap=false) public abstract class WorldBattleArmorQuestFusionNativeMixin {
 @Shadow private WeaponfusionRecipe selectedRecipe;
 @Inject(method="m_142365_",at=@At("HEAD"),remap=false) private void jem$begin(Player player,ItemStack taken,CallbackInfo ci){WorldBattleArmorQuestFusion.taking((AbstractContainerMenu)(Object)this,selectedRecipe,player,taken);}
 @Inject(method="m_142365_",at=@At("RETURN"),remap=false) private void jem$end(Player player,ItemStack taken,CallbackInfo ci){WorldBattleArmorQuestFusion.taken((AbstractContainerMenu)(Object)this,player);}
}
