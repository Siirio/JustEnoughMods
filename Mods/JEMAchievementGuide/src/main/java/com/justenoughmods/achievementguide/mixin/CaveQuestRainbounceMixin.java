package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.item.RainbounceBootsItem.class,remap=false)
public abstract class CaveQuestRainbounceMixin {
    @Unique private static final ThreadLocal<Double> jem$caveBefore=new ThreadLocal<>();
    @Inject(method="onEntityLand",at=@At("HEAD"))private static void jem$before(LivingEntity actor,Vec3 fall,CallbackInfo ci){jem$caveBefore.set(actor.getDeltaMovement().y);}
    @Inject(method="onEntityLand",at=@At("RETURN"))private static void jem$after(LivingEntity actor,Vec3 fall,CallbackInfo ci){
        Double before=jem$caveBefore.get();jem$caveBefore.remove();
        if(actor instanceof ServerPlayer player && CaveQuestProof.actor(player)!=null && before!=null && fall.y<0 && player.getDeltaMovement().y>before
                && player.getDeltaMovement().y>0 && CaveQuestProof.item(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET),"rainbounce_boots"))CaveQuestMotion.of(player).bounce(player);
    }
}
