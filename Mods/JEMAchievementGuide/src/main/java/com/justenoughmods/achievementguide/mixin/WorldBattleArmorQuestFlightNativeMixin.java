package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.item.ItemStack;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.github.L_Ender.cataclysm.items.Ignitium_Elytra_ChestPlate",remap=false) public abstract class WorldBattleArmorQuestFlightNativeMixin {
 @Inject(method="elytraFlightTick",at=@At("RETURN"),remap=false) private void jem$flight(ItemStack stack,LivingEntity entity,int ticks,CallbackInfoReturnable<Boolean> ci){var actor=WorldBattleArmorQuestProof.real(entity);if(actor!=null)WorldBattleArmorQuestMotion.flight(actor,stack,ci.getReturnValue());}
}
