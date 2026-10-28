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

@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.item.GumballEntity.class,remap=false)
public abstract class CaveQuestGumballBounceMixin {
    @Unique private int jem$wallBefore;
    @Unique private boolean jem$solidWall;
    @Inject(method="m_6532_",at=@At("HEAD"))
    private void jem$beforeWall(net.minecraft.world.phys.HitResult hit,CallbackInfo ci){
        var ball=(com.github.alexmodguy.alexscaves.server.entity.item.GumballEntity)(Object)this;
        jem$wallBefore=ball.getBounces(); jem$solidWall=false;
        if(ball.level() instanceof net.minecraft.server.level.ServerLevel level && hit instanceof net.minecraft.world.phys.BlockHitResult block
                && CaveQuestProof.loaded(level,block.getBlockPos())) jem$solidWall=!level.getBlockState(block.getBlockPos()).getCollisionShape(level,block.getBlockPos()).isEmpty();
    }
    @Inject(method="m_6532_",at=@At("RETURN"))
    private void jem$afterWall(net.minecraft.world.phys.HitResult hit,CallbackInfo ci){
        var ball=(com.github.alexmodguy.alexscaves.server.entity.item.GumballEntity)(Object)this;
        if(jem$solidWall && ball.getBounces()>jem$wallBefore && !ball.isRemoved()) ball.getPersistentData().putBoolean("jemCaveWallBounceV4",true);
    }
}
