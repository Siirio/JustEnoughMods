package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.talhanation.smallships.world.entity.projectile.Cannon;
import com.talhanation.smallships.world.entity.projectile.CannonBallEntity;
import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Cannon.class, remap = false)
public abstract class ShipCannonFireQuestMixin {
    @Shadow @Final private Ship ship;
    @Unique private ServerPlayer jem$cannonActor;
    @Unique private int jem$beforeAmmo;
    @Unique private CannonBallEntity jem$spawnedBall;

    @Unique private int jem$ammo() {
        if (ship instanceof ContainerEntity container && container.getItemStacks().size() > 4096) return -1;
        Iterable<ItemStack> stacks = ship instanceof ContainerEntity container ? container.getItemStacks()
                : jem$cannonActor.getInventory().items;
        int count = 0;
        for (ItemStack stack : stacks) if (stack.is(ModItems.CANNON_BALL)) count += stack.getCount();
        return count;
    }

    @Inject(method = "shoot(Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/entity/LivingEntity;DD)V", at = @At("HEAD"))
    private void jem$beforeShot(Vec3 direction, double y, LivingEntity driver, double speed, double accuracy, CallbackInfo callback) {
        jem$cannonActor = null;
        jem$spawnedBall = null;
        if (driver instanceof ServerPlayer actor && TravelProof.controller(ship) == actor && !ship.isSunken()) {
            jem$cannonActor = actor;
            jem$beforeAmmo = jem$ammo();
        }
    }

    @Redirect(method = "shoot(Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/entity/LivingEntity;DD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z", remap = false))
    private boolean jem$acceptedProjectile(Level level, Entity projectile) {
        boolean accepted = level.addFreshEntity(projectile);
        if (accepted && jem$cannonActor != null && projectile instanceof CannonBallEntity ball
                && ball.getOwner() == jem$cannonActor && ball.level() == ship.level()) jem$spawnedBall = ball;
        return accepted;
    }

    @Inject(method = "shoot(Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/entity/LivingEntity;DD)V", at = @At("RETURN"))
    private void jem$afterShot(Vec3 direction, double y, LivingEntity driver, double speed, double accuracy, CallbackInfo callback) {
        if (jem$cannonActor != null && jem$beforeAmmo > 0 && jem$spawnedBall != null && TravelProof.controller(ship) == jem$cannonActor
                && jem$beforeAmmo - jem$ammo() == 1) {
            var proof = jem$spawnedBall.getPersistentData();
            proof.putUUID("JemTravelCannonActorV4", jem$cannonActor.getUUID());
            proof.putUUID("JemTravelCannonShipV4", ship.getUUID());
            proof.putUUID("JemTravelCannonProjectileV4", jem$spawnedBall.getUUID());
        }
        jem$cannonActor = null;
        jem$spawnedBall = null;
    }
}
