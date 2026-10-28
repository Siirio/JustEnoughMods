package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.GummyBearEntity", remap = false)
public abstract class RemainingGummyAlchemyQuestMixin {
    @Unique private static final String jem$key = "JemQuestGummyDigestionV4";
    @Shadow private ResourceLocation digestingEffect;
    @Shadow private int sleepFor;
    @Shadow public abstract boolean isDigesting();
    @Shadow public abstract boolean isBearSleeping();
    @Unique private ItemStack jem$fed;
    @Unique private int jem$count;
    @Unique private ResourceLocation jem$potion;
    @Unique private boolean jem$wasDigesting;

    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$feedStart(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$fed = null;
        var actor = RemainingFoodActors.real(player);
        if (actor == null) return;
        ItemStack held = actor.getItemInHand(hand);
        if (!held.isEmpty() && !PotionUtils.getPotion(held).getEffects().isEmpty()) {
            jem$fed = held;
            jem$count = held.getCount();
            jem$potion = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(held));
            jem$wasDigesting = isDigesting();
        }
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$acceptedPotion(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (RemainingFoodActors.real(player) != null && jem$fed != null && !jem$wasDigesting && isDigesting()
                && sleepFor > 0 && jem$count - jem$fed.getCount() == 1 && jem$potion != null && jem$potion.equals(digestingEffect)) {
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", player.getUUID());
            proof.putUUID("bear", ((Entity)(Object)this).getUUID());
            proof.putString("potion", jem$potion.toString());
            ((Entity)(Object)this).getPersistentData().put(jem$key, proof);
        }
        jem$fed = null;
        jem$potion = null;
    }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$nativeHibernation(CallbackInfo callback) {
        Entity bear = (Entity)(Object)this;
        if (!bear.level().isClientSide && isBearSleeping()) {
            CompoundTag proof = bear.getPersistentData().getCompound(jem$key);
            if (proof.hasUUID("bear") && proof.getUUID("bear").equals(bear.getUUID())) proof.putBoolean("slept", true);
        }
    }
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE",
            target = "Lcom/github/alexmodguy/alexscaves/server/entity/living/GummyBearEntity;m_5552_(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$nativeBean(@Coerce Entity bear, ItemStack bean, float height) {
        ItemEntity output = bear.spawnAtLocation(bean, height);
        CompoundTag proof = bear.getPersistentData().getCompound(jem$key);
        if (output != null && bear.level() instanceof ServerLevel level && level.getEntity(output.getUUID()) == output
                && proof.hasUUID("actor") && proof.hasUUID("bear") && proof.getUUID("bear").equals(bear.getUUID())
                && proof.getBoolean("slept") && !isBearSleeping() && sleepFor <= 0 && isDigesting()
                && RemainingFoodActors.item(bean).equals("alexscaves:jelly_bean")
                && proof.getString("potion").equals(String.valueOf(ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(bean))))
                && !PotionUtils.getPotion(bean).getEffects().isEmpty()) {
            FoodDroppedProof.update(output, "v3_cave_gummy_alchemy", output.getItem().getCount(), proof.getUUID("actor"));
        }
        return output;
    }
}
