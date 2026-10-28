package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PotionItem.class)
public abstract class WildlifeQuestDraughtDrinkMixin {
    @Unique private final ThreadLocal<ItemStack> jem$before = new ThreadLocal<>();
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void jem$before(ItemStack stack, Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> callback) { jem$before.set(stack.copy()); }
    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void jem$after(ItemStack stack, Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack before = jem$before.get(); jem$before.remove(); if (before == null || !(user instanceof Player player)) return; var actor = WildlifeQuestProof.real(player);
        if (actor != null && WildlifeQuestBrews.draught(before) && WildlifeQuestProof.debited(before, stack, 1)
                && NativeFoodSources.get(actor.serverLevel()).consume(actor.getUUID(), NativeFoodSources.spent(before, 1), "wildlife_draught", 1)) WildlifeQuestProof.emit(actor, "v3_fauna_field_draughts");
    }
}
