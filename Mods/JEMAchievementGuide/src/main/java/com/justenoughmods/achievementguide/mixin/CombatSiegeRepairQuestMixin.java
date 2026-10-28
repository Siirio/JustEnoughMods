package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.combattools.CombatToolProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.talhanation.siegeweapons.entities.AbstractVehicleEntity", remap = false)
public abstract class CombatSiegeRepairQuestMixin {
    @Shadow public abstract float getHealth();
    @Shadow public abstract double getMaxHealth();
    @Unique private float jem$health;
    @Unique private int jem$nuggets, jem$planks;
    @Unique private ItemStack jem$material = ItemStack.EMPTY;
    @Unique private boolean jem$iron;
    @Unique private int jem$countPlanks(Player actor) {
        int count = 0;
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
            ItemStack stack = actor.getInventory().getItem(slot);
            if (stack.is(ItemTags.PLANKS)) count += stack.getCount();
        }
        return count;
    }
    @Inject(method = "interactIronNuggets", at = @At("HEAD"))
    private void jem$begin(Player actor, CallbackInfoReturnable<Boolean> callback) {
        jem$health = getHealth(); jem$material = actor.getMainHandItem(); jem$iron = jem$material.is(Items.IRON_NUGGET); jem$nuggets = jem$material.getCount(); jem$planks = jem$countPlanks(actor);
    }
    @Inject(method = "interactIronNuggets", at = @At("RETURN"))
    private void jem$end(Player actor, CallbackInfoReturnable<Boolean> callback) {
        String type = CombatToolProof.type((Entity)(Object)this);
        if (CombatToolProof.real(actor) && (type.equals("siegeweapons:ballista") || type.equals("siegeweapons:catapult"))
                && callback.getReturnValue() && jem$health < getMaxHealth() && getHealth() > jem$health && jem$iron
                && jem$nuggets - jem$material.getCount() == 1 && jem$planks - jem$countPlanks(actor) == 1) CombatToolProof.emit((ServerPlayer)actor, "siege_repair");
        jem$material = ItemStack.EMPTY;
    }
}
