package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=com.github.alexmodguy.alexscaves.server.item.RaygunItem.class,remap=false)
public abstract class CaveQuestRaygunMixin {
    @Unique private static final ThreadLocal<int[]> jem$reload=new ThreadLocal<>();
    @Unique private static int jem$rods(net.minecraft.world.entity.player.Player player){int n=0;for(int slot=0;slot<player.getInventory().getContainerSize();slot++){var stack=player.getInventory().getItem(slot);if(com.github.alexmodguy.alexscaves.server.item.RaygunItem.AMMO.test(stack))n+=stack.getCount();}return n;}
    @Inject(method="m_7203_",at=@At("HEAD"))
    private void jem$beforeReload(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack>> ci){
        jem$reload.set(new int[]{com.github.alexmodguy.alexscaves.server.item.RaygunItem.getCharge(player.getItemInHand(hand)),jem$rods(player)});
    }
    @Inject(method="m_7203_",at=@At("RETURN"))
    private void jem$afterReload(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack>> ci){
        int[] before=jem$reload.get();jem$reload.remove();
        if(before!=null && before[1]-jem$rods(player)==1 && com.github.alexmodguy.alexscaves.server.item.RaygunItem.getCharge(player.getItemInHand(hand))<before[0])CaveQuestProof.emit(player,"v3_cave_raygun_reload");
    }
    @Redirect(method="m_5929_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$nativeRay(Entity target,DamageSource source,float amount,net.minecraft.world.level.Level level,LivingEntity user,net.minecraft.world.item.ItemStack item,int time){
        return CaveQuestProof.damage(target,source,amount,user,"v3_cave_raygun");
    }
}
