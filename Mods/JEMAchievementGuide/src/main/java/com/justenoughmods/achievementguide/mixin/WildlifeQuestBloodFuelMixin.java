package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.item.ItemBloodSprayer", "com.github.alexthe666.alexsmobs.item.ItemHemolymphBlaster"}, remap = false)
public abstract class WildlifeQuestBloodFuelMixin {
    @Unique private final ThreadLocal<WildlifeQuestBlood.Run> jem$reload = new ThreadLocal<>();
    @Inject(method = "m_7203_", at = @At("HEAD"))
    private void jem$beforeUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) { jem$reload.set(WildlifeQuestBlood.begin(player, player.getItemInHand(hand))); }
    @Inject(method = "m_7203_", at = @At("RETURN"))
    private void jem$used(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) { var run = jem$reload.get(); jem$reload.remove(); WildlifeQuestBlood.end(run); }
    @Inject(method = "m_5929_", at = @At("HEAD"))
    private void jem$beforeTick(Level level, LivingEntity user, ItemStack stack, int count, CallbackInfo callback) { jem$reload.set(WildlifeQuestBlood.begin(user, stack)); }
    @Inject(method = "m_5929_", at = @At("RETURN"))
    private void jem$ticked(Level level, LivingEntity user, ItemStack stack, int count, CallbackInfo callback) { var run = jem$reload.get(); jem$reload.remove(); WildlifeQuestBlood.end(run); }
    @Inject(method = "findAmmo", at = @At("RETURN"))
    private void jem$ammo(Player player, CallbackInfoReturnable<ItemStack> callback) { var run = jem$reload.get(); if (run != null && run.actor == player) { run.ammo = callback.getReturnValue(); run.ammoBefore = callback.getReturnValue().copy(); } }
}
