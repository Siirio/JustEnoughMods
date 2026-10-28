package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.justenoughmods.achievementguide.integration.travel.TravelVehicleObservation;
import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.entity.ship.abilities.Sailable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Ship.class, remap = false)
public abstract class ShipTravelQuestMixin implements TravelVehicleObservation {
    @Unique private TravelProof.Journey jem$sailing;
    @Unique private byte jem$beforeSail;
    @Unique private ServerPlayer jem$repairActor;
    @Unique private float jem$beforeDamage;
    @Unique private int jem$beforeNuggets;
    @Unique private int jem$beforePlanks;

    @Unique private Ship jem$ship() { return (Ship) (Object) this; }

    @Inject(method = "setData(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V", at = @At("HEAD"))
    private void jem$beforeSail(EntityDataAccessor<?> accessor, Object value, CallbackInfo callback) {
        if (accessor == Ship.SAIL_STATE && jem$ship() instanceof Sailable sail) jem$beforeSail = sail.getSailState();
    }

    @Inject(method = "setData(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V", at = @At("RETURN"))
    private void jem$afterSail(EntityDataAccessor<?> accessor, Object value, CallbackInfo callback) {
        var ship = jem$ship();
        if (accessor != Ship.SAIL_STATE || ship.level().isClientSide || !(ship instanceof Sailable sail)) return;
        byte state = sail.getSailState();
        if (state < 1 || state > 4) {
            jem$sailing = null;
        } else if (jem$beforeSail == 0) {
            ServerPlayer actor = TravelProof.controller(ship);
            jem$sailing = actor != null && ship.isInWater() && !ship.isUnderWater() && !ship.isSunken()
                    ? new TravelProof.Journey(actor, ship.position(), ship) : null;
        }
    }

    @Inject(method = "m_8119_()V", at = @At("TAIL"), remap = false)
    private void jem$journeyLifecycle(CallbackInfo callback) {
        var ship = jem$ship();
        if (ship.level().isClientSide || jem$sailing == null) return;
        if (TravelProof.controller(ship) != jem$sailing.actor || !ship.isInWater() || ship.isSunken()
                || ship.isUnderWater() || ship.tickCount - jem$sailing.began > 24000
                || !(ship instanceof Sailable sail) || sail.getSailState() < 1 || sail.getSailState() > 4) jem$sailing = null;
    }

    @Override public void jem$travelAccepted(ServerPlayer actor, Vec3 from, boolean groundedBefore) {
        var ship = jem$ship();
        if (jem$sailing == null) return;
        if (!jem$sailing.valid(ship, actor, from) || !ship.isInWater() || ship.isUnderWater() || ship.isSunken()
                || !(ship instanceof Sailable sail) || sail.getSailState() < 1 || sail.getSailState() > 4) {
            jem$sailing = null;
            return;
        }
        jem$sailing.last = ship.position();
        if (jem$sailing.reached(ship)) {
            TravelProof.emit(actor, "v3_world_ship_sailing");
            jem$sailing = null;
        }
    }

    @Override public void jem$travelReset() { jem$sailing = null; }

    @Unique private static int jem$materialCount(ServerPlayer actor, boolean planks) {
        int count = 0;
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
            ItemStack stack = actor.getInventory().getItem(slot);
            if (planks ? stack.is(ItemTags.PLANKS) : stack.is(Items.IRON_NUGGET)) count += stack.getCount();
        }
        return count;
    }

    @Inject(method = "interactIronNuggets(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"))
    private void jem$beforeRepair(Player player, CallbackInfoReturnable<Boolean> callback) {
        jem$repairActor = null;
        var ship = jem$ship();
        if (player instanceof ServerPlayer actor && TravelProof.eligible(actor) && actor.level() == ship.level()
                && !ship.hasPassenger(actor) && !ship.isSunken() && !ship.isRemoved()
                && actor.getMainHandItem().is(Items.IRON_NUGGET)) {
            jem$beforeDamage = ship.getDamage();
            if (Float.isFinite(jem$beforeDamage) && jem$beforeDamage > 0) {
                jem$repairActor = actor;
                jem$beforeNuggets = jem$materialCount(actor, false);
                jem$beforePlanks = jem$materialCount(actor, true);
            }
        }
    }

    @Inject(method = "interactIronNuggets(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("RETURN"))
    private void jem$afterRepair(Player player, CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer actor = jem$repairActor;
        jem$repairActor = null;
        var ship = jem$ship();
        if (actor == player && actor != null && callback.getReturnValue() && !ship.isRemoved()
                && Float.isFinite(ship.getDamage()) && ship.getDamage() < jem$beforeDamage
                && jem$beforeNuggets - jem$materialCount(actor, false) == 1
                && jem$beforePlanks - jem$materialCount(actor, true) == 1) {
            TravelProof.emit(actor, "v3_world_ship_repair");
        }
    }
}
