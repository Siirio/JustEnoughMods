package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneControlProof;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class CombatRuneControlQuestMixin {
    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void jem$before(Vec3 force, CallbackInfo callback) { CombatRuneControlProof.begin((Entity)(Object)this); }
    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void jem$after(Vec3 force, CallbackInfo callback) { CombatRuneControlProof.end((Entity)(Object)this); }
    @Inject(method = "move", at = @At("RETURN"))
    private void jem$moved(MoverType type, Vec3 movement, CallbackInfo callback) { if (!((Object)this instanceof ServerPlayer)) CombatRuneControlProof.moved((Entity)(Object)this); }
    @Inject(method = "discard", at = @At("RETURN"))
    private void jem$blocked(CallbackInfo callback) { if ((Object)this instanceof Projectile projectile && projectile.isRemoved()) CombatRuneProof.committed(projectile); }
}
