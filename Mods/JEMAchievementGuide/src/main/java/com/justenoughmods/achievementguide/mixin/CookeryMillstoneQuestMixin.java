package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.MillstoneBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.MillstoneRecipe;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.integration.food.MillstoneFoodProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MillstoneBlockEntity.class, remap = false)
public abstract class CookeryMillstoneQuestMixin {
    @Unique private boolean jem$flourRecipe;
    @Inject(method = "lambda$tick$4", at = @At("HEAD"))
    private void jem$recipe(MillstoneRecipe recipe, CallbackInfo callback) {
        MillstoneBlockEntity owner = (MillstoneBlockEntity)(Object)this;
        MillstoneFoodProof.clear(owner);
        jem$flourRecipe = owner.hasEntity() && owner.isOutputEmpty()
                && FoodOutputProof.item(owner.getInput(), "minecraft:wheat")
                && recipe.id().toString().equals("kaleidoscope_cookery:millstone/flour_from_wheat");
    }
    @Inject(method = "lambda$tick$4", at = @At("RETURN"))
    private void jem$committed(MillstoneRecipe recipe, CallbackInfo callback) {
        MillstoneBlockEntity owner = (MillstoneBlockEntity)(Object)this;
        if (jem$flourRecipe && owner.getInput().isEmpty()) MillstoneFoodProof.certify(owner);
        jem$flourRecipe = false;
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeEjection(ServerLevel level, Entity entity) {
        MillstoneFoodProof.beforeInsertion((MillstoneBlockEntity)(Object)this, entity);
        return level.addFreshEntity(entity);
    }
    @Inject(method = "resetWhenTakeout", at = @At("HEAD"))
    private void jem$reset(CallbackInfo callback) { MillstoneFoodProof.clear((BlockEntity)(Object)this); }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { MillstoneFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { MillstoneFoodProof.load((BlockEntity)(Object)this, tag); }
}
