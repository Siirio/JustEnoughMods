package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.item.CandyCaneHookEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=CandyCaneHookEntity.class,remap=false)
public abstract class CaveQuestHookMixin {
    @Unique private Vec3 jem$hookVelocity;
    @Inject(method="m_8060_",at=@At("RETURN"))private void jem$anchor(BlockHitResult hit,CallbackInfo ci){
        var hook=(CandyCaneHookEntity)(Object)this;
        if(hook.level() instanceof ServerLevel level && CaveQuestProof.loaded(level,hit.getBlockPos()) && !level.getBlockState(hit.getBlockPos()).isAir() && hook.getHookedPosition()!=null){
            var proof=new CompoundTag();proof.putLong("pos",hit.getBlockPos().asLong());proof.put("state",net.minecraft.nbt.NbtUtils.writeBlockState(level.getBlockState(hit.getBlockPos())));hook.getPersistentData().put("jemCaveAnchorV4",proof);
        }
    }
    @Inject(method="fling",at=@At("HEAD"))private void jem$before(CallbackInfo ci){var hook=(CandyCaneHookEntity)(Object)this;jem$hookVelocity=hook.getOwner()==null?null:hook.getOwner().getDeltaMovement();}
    @Inject(method="fling",at=@At("RETURN"))private void jem$flung(CallbackInfo ci){
        var hook=(CandyCaneHookEntity)(Object)this;var actor=CaveQuestProof.actor(hook.getOwner());
        var proof=hook.getPersistentData().getCompound("jemCaveAnchorV4");var pos=BlockPos.of(proof.getLong("pos"));
        if(actor!=null && jem$hookVelocity!=null && hook.getHookedEntity()==null && hook.getHookedPosition()!=null && proof.contains("state",10)
                && CaveQuestProof.loaded(actor.serverLevel(),pos) && net.minecraft.nbt.NbtUtils.writeBlockState(actor.serverLevel().getBlockState(pos)).equals(proof.getCompound("state"))
                && actor.getDeltaMovement().distanceToSqr(jem$hookVelocity)>1.0e-8)CaveQuestMotion.of(actor).hook(actor);
        jem$hookVelocity=null;
    }
}
