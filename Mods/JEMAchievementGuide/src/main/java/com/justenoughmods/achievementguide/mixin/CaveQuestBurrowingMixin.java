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

@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.item.BurrowingArrowEntity.class,remap=false)
public abstract class CaveQuestBurrowingMixin {
    @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean jem$nativeMining(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,boolean drops){
        var arrow=(com.github.alexmodguy.alexscaves.server.entity.item.BurrowingArrowEntity)(Object)this;
        var actor=CaveQuestProof.actor(arrow.getOwner());
        boolean observed=actor!=null && CaveQuestProof.permitted(actor,pos) && !level.getBlockState(pos).isAir();
        boolean result=level.destroyBlock(pos,drops);
        if(observed && result && level.getBlockState(pos).isAir()) CaveQuestProof.emit(actor,"v3_cave_shadow_arsenal_burrow");
        return result;
    }
}
