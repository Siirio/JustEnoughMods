package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareRide;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.imasillylittleguy.cnc.entity.CaribouEntity", remap = false)
public abstract class CreatureCareCaribouMixin implements CreatureCareRide.Saddle {
    @Shadow @Final public static EntityDataAccessor<Boolean> DATA_saddle;
    @Override public boolean jem$careSaddled() { return ((Entity)(Object)this).getEntityData().get(DATA_saddle); }
    @Unique private Vec3 jem$before;
    @Unique private ItemStack jem$food = ItemStack.EMPTY;
    @Unique private boolean jem$wild, jem$saddled;
    @Unique private int jem$saddles;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$feedBefore(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$food = player.getItemInHand(hand).copy(); jem$wild = !((TamableAnimal)(Object)this).isTame();
        jem$saddled = jem$careSaddled(); jem$saddles = player.getInventory().countItem(Items.SADDLE);
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$feedAfter(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var actor = CreatureCareProof.real(player); var self = (TamableAnimal)(Object)this;
        if (CreatureCareProof.owned(self, actor)) {
            if (jem$wild && jem$food.is(Items.RED_MUSHROOM) && CreatureCareProof.debited(jem$food, player.getItemInHand(hand))) {
                CreatureCareProof.mark(self, "caribou_food_owner", actor.getUUID());
            }
            if (!jem$saddled && jem$careSaddled() && jem$food.is(Items.SADDLE)
                    && jem$saddles - player.getInventory().countItem(Items.SADDLE) == 1) CreatureCareProof.mark(self, "caribou_saddle_actor", actor.getUUID());
        }
        jem$food = ItemStack.EMPTY;
    }
    @Inject(method = "m_7023_", at = @At("HEAD"))
    private void jem$before(Vec3 input, CallbackInfo callback) {
        Entity self = (Entity)(Object)this;
        jem$before = !self.level().isClientSide && self.getFirstPassenger() instanceof ServerPlayer ? self.position() : null;
    }
    @Inject(method = "m_7023_", at = @At("RETURN"))
    private void jem$after(Vec3 input, CallbackInfo callback) {
        TamableAnimal self = (TamableAnimal)(Object)this;
        if (self.getFirstPassenger() instanceof ServerPlayer actor) CreatureCareRide.caribou(actor, self, jem$before);
        jem$before = null;
    }
}
