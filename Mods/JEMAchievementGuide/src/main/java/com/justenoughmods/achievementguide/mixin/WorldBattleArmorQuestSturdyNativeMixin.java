package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraft.world.item.ItemStack;import top.theillusivec4.curios.api.SlotContext;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.github.L_Ender.cataclysm.items.CuriosItem.Sturdy_Boots",remap=false) public abstract class WorldBattleArmorQuestSturdyNativeMixin {
 @Inject(method="canWalkOnPowderedSnow",at=@At("RETURN"),remap=false) private void jem$walk(SlotContext context,ItemStack stack,CallbackInfoReturnable<Boolean> ci){WorldBattleArmorQuestMotion.sturdy(context.entity(),stack,ci.getReturnValue());}
}
