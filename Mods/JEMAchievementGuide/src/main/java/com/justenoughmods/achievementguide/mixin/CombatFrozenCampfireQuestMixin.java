package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatCampfireProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CampfireBlockEntity.class)
public abstract class CombatFrozenCampfireQuestMixin {
    @Inject(method = "placeFood", at = @At("HEAD"))
    private void jem$placeBegin(Entity actor, ItemStack food, int duration, CallbackInfoReturnable<Boolean> callback) { CombatCampfireProof.placeBegin((CampfireBlockEntity)(Object)this, actor, food); }
    @Inject(method = "placeFood", at = @At("RETURN"))
    private void jem$placeEnd(Entity actor, ItemStack food, int duration, CallbackInfoReturnable<Boolean> callback) { CombatCampfireProof.placeEnd(callback.getReturnValue()); }
    @Inject(method = "cookTick", at = @At("HEAD"))
    private static void jem$cookBegin(Level level, BlockPos pos, BlockState state, CampfireBlockEntity owner, CallbackInfo callback) { CombatCampfireProof.cookBegin(owner); }
    @Inject(method = "lambda$cookTick$0(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/CampfireCookingRecipe;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private static void jem$assembled(Container input, Level level, CampfireCookingRecipe recipe, CallbackInfoReturnable<ItemStack> callback) { CombatCampfireProof.assembled(input, level, recipe, callback.getReturnValue()); }
    @Inject(method = "cookTick", at = @At("RETURN"))
    private static void jem$cookEnd(Level level, BlockPos pos, BlockState state, CampfireBlockEntity owner, CallbackInfo callback) { CombatCampfireProof.cookEnd(); }
}
