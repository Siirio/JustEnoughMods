package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.item.MothDustItem.class,remap=false)
public abstract class CaveQuestMothDustMixin {
    @Unique private static final ThreadLocal<int[]> jem$caveDust=new ThreadLocal<>();
    @Inject(method="m_5551_",at=@At("HEAD"))private void jem$before(ItemStack dust,Level level,LivingEntity user,int remaining,CallbackInfo ci){jem$caveDust.set(new int[]{dust.getCount(),0,com.github.alexmodguy.alexscaves.server.item.MothDustItem.getPowerForTime(dust.getUseDuration()-remaining)>0?1:0});}
    @Redirect(method="m_5551_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Mob;m_6710_(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void jem$nativeMark(Mob mob,LivingEntity target){var before=mob.getTarget();mob.setTarget(target);var run=jem$caveDust.get();if(run!=null && !(target instanceof net.minecraft.world.entity.player.Player) && before!=target && mob.getTarget()==target)run[1]=1;}
    @Inject(method="m_5551_",at=@At("RETURN"))private void jem$after(ItemStack dust,Level level,LivingEntity user,int remaining,CallbackInfo ci){
        int[] run=jem$caveDust.get();jem$caveDust.remove();
        if(run!=null && run[1]==1 && run[0]-dust.getCount()==1 && run[2]==1)CaveQuestProof.emit(user,"v3_cave_moth_lighting");
    }
}
