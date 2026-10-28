package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One native mount, one continuously controlling real owner, two seconds of actual flight. */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.SubterranodonEntity", remap = false)
public abstract class SubterranodonFlightQuestMixin {
    @Unique private UUID jem$flightActor;
    @Unique private int jem$flightTicks;
    @Unique private Vec3 jem$beforeFlight;
    @Unique private ServerPlayer jem$beforeRider;

    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$beforeFlightTick(CallbackInfo callback) {
        TamableAnimal mount = (TamableAnimal) (Object) this;
        if (mount.level().isClientSide) return;
        jem$beforeFlight = mount.position();
        jem$beforeRider = jem$controller(mount);
    }

    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$afterFlightTick(CallbackInfo callback) {
        TamableAnimal mount = (TamableAnimal) (Object) this;
        if (mount.level().isClientSide) return;
        ServerPlayer rider = jem$controller(mount);
        Vec3 before = jem$beforeFlight;
        ServerPlayer started = jem$beforeRider;
        jem$beforeFlight = null;
        jem$beforeRider = null;
        double moved = before == null ? 0 : mount.position().distanceToSqr(before);
        if (rider == null || rider != started || !Double.isFinite(moved) || moved <= 1.0E-8) {
            jem$flightActor = null;
            jem$flightTicks = 0;
            return;
        }
        if (!rider.getUUID().equals(jem$flightActor)) {
            jem$flightActor = rider.getUUID();
            jem$flightTicks = 0;
        }
        if (jem$flightTicks < 40 && ++jem$flightTicks == 40) {
            QuestActionTrigger.INSTANCE.emit(rider, "v3_cave_dinosaur_fieldwork");
        }
    }

    @Unique private ServerPlayer jem$controller(TamableAnimal mount) {
        if (!mount.isAlive() || mount.onGround() || !((FlyingAnimal) this).isFlying()
                || !(mount.getControllingPassenger() instanceof ServerPlayer rider) || !rider.isAlive()
                || !mount.isOwnedBy(rider) || rider.getVehicle() != mount
                || rider.isSpectator() || rider.getAbilities().instabuild
                || rider instanceof net.minecraftforge.common.util.FakePlayer) return null;
        return rider;
    }
}
