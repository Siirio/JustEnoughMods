package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatStationProof;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({AbstractFurnaceBlockEntity.class, BrewingStandBlockEntity.class})
public abstract class CombatStationReplacementQuestMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$set(int slot, ItemStack item, CallbackInfo callback) { CombatStationProof.inserted((BlockEntity)(Object)this, slot, item); }
    @Inject(method = "removeItem", at = @At("HEAD"))
    private void jem$remove(int slot, int count, CallbackInfoReturnable<ItemStack> callback) { CombatStationProof.replaced((BlockEntity)(Object)this, slot); }
    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"))
    private void jem$take(int slot, CallbackInfoReturnable<ItemStack> callback) { CombatStationProof.replaced((BlockEntity)(Object)this, slot); }
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void jem$clear(CallbackInfo callback) { CombatStationProof.clear((BlockEntity)(Object)this); }
}
