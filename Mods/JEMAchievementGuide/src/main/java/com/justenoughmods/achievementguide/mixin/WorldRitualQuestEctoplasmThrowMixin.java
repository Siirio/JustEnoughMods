package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.level.Level;import net.minecraft.world.item.ItemStack;import net.minecraft.world.entity.LivingEntity;
@Mixin(value=net.mcreator.luminousnether.item.EctoplasmBallItem.class,remap=false)
public abstract class WorldRitualQuestEctoplasmThrowMixin {
    @Inject(method="m_5551_",at=@At("HEAD"))private void jem$before(ItemStack stack,Level world,LivingEntity actor,int time,CallbackInfo ci){WorldRitualQuestEctoplasm.throwBegin(actor);}
    @Inject(method="findAmmo",at=@At("RETURN"))private void jem$ammo(net.minecraft.world.entity.player.Player actor,CallbackInfoReturnable<ItemStack> ci){WorldRitualQuestEctoplasm.ammo(ci.getReturnValue());}
    @Inject(method="m_5551_",at=@At("RETURN"))private void jem$after(ItemStack stack,Level world,LivingEntity actor,int time,CallbackInfo ci){WorldRitualQuestEctoplasm.end();}
}
