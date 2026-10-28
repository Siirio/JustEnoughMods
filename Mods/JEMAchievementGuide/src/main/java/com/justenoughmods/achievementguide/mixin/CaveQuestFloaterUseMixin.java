package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=com.github.alexmodguy.alexscaves.server.item.FloaterItem.class,remap=false)
public abstract class CaveQuestFloaterUseMixin {
    @Unique private static final ThreadLocal<Integer> jem$caveBefore=new ThreadLocal<>();
    @Inject(method="m_7203_",at=@At("HEAD"))private void jem$before(Level level,Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){jem$caveBefore.set(user.getItemInHand(hand).getCount());}
    @Inject(method="m_7203_",at=@At("RETURN"))private void jem$after(Level level,Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){
        Integer before=jem$caveBefore.get();jem$caveBefore.remove();var actor=CaveQuestProof.actor(user);
        if(actor!=null && before!=null && before-user.getItemInHand(hand).getCount()==1 && user.getRootVehicle() instanceof com.github.alexmodguy.alexscaves.server.entity.item.FloaterEntity floater
                && actor.serverLevel().getEntity(floater.getUUID())==floater){floater.getPersistentData().putUUID("jemCaveFloaterV4",actor.getUUID());floater.getPersistentData().putDouble("jemCaveFloaterYV4",actor.getY());}
    }
}
