package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.item.TotemOfPossessionItem.class,remap=false)
public abstract class CaveQuestTotemMixin {
    @Shadow private Entity getControlledEntity(Level level,ItemStack item){throw new AssertionError();}
    @Redirect(method="m_5929_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;m_26519_(DDDD)Z"))
    private boolean jem$actualPossession(PathNavigation navigation,double x,double y,double z,double speed,Level level,LivingEntity user,ItemStack totem,int remaining){
        var actor=CaveQuestProof.actor(user);var entity=getControlledEntity(level,totem);
        boolean observed=actor!=null && entity instanceof Mob mob && mob.getNavigation()==navigation && entity.isAlive() && entity.getUUID().equals(com.github.alexmodguy.alexscaves.server.item.TotemOfPossessionItem.getBoundEntityUUID(totem));
        if(observed)CaveQuestMotion.of(actor).totemBefore(actor,(Mob)entity);
        boolean result=navigation.moveTo(x,y,z,speed);
        if(observed && result && speed>0)CaveQuestMotion.of(actor).totemAfter((Mob)entity);
        return result;
    }
}
