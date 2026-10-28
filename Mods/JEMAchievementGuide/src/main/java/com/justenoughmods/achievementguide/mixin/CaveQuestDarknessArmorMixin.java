package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.item.DarknessArmorItem;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=DarknessArmorItem.class,remap=false)
public abstract class CaveQuestDarknessArmorMixin {
    @Unique private static final ThreadLocal<Boolean> jem$caveCharged=new ThreadLocal<>();
    @Inject(method="onKeyPacket",at=@At("HEAD"))private void jem$before(Entity user,ItemStack cloak,int key,CallbackInfo ci){
        var actor=CaveQuestProof.actor(user);jem$caveCharged.set(actor!=null && key==2 && DarknessArmorItem.hasMeter(actor) && DarknessArmorItem.getMeterProgress(cloak)>=1 && !actor.hasEffect(ACEffectRegistry.DARKNESS_INCARNATE.get()));
    }
    @Inject(method="onKeyPacket",at=@At("RETURN"))private void jem$after(Entity user,ItemStack cloak,int key,CallbackInfo ci){
        Boolean before=jem$caveCharged.get();jem$caveCharged.remove();var actor=CaveQuestProof.actor(user);
        if(Boolean.TRUE.equals(before) && actor!=null && actor.hasEffect(ACEffectRegistry.DARKNESS_INCARNATE.get()) && DarknessArmorItem.getMeterProgress(cloak)==0)CaveQuestMotion.of(actor).flight(actor);
    }
}
