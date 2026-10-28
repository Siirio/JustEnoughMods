package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level; import net.minecraft.world.entity.LivingEntity;
@Mixin(value = com.github.L_Ender.cataclysm.items.Cursed_bow.class, remap = false)
public abstract class BattleQuestCursedbowMixin {
    @Inject(method="m_5551_", at=@At("HEAD"))
    private void jem$begin(ItemStack stack, Level level, LivingEntity actor, int remaining, CallbackInfo ci) { BattleQuestArtifacts.begin(actor, stack, "ranged"); }
    @Inject(method="m_5551_", at=@At("RETURN"))
    private void jem$end(ItemStack stack, Level level, LivingEntity actor, int remaining, CallbackInfo ci) { BattleQuestArtifacts.end(); }
    @Redirect(method="m_5551_",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ArrowItem;isInfinite(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean jem$ammo(net.minecraft.world.item.ArrowItem arrow, ItemStack ammo, ItemStack bow, net.minecraft.world.entity.player.Player actor) {
        boolean free=arrow.isInfinite(ammo,bow,actor); BattleQuestArtifacts.ammo(ammo,free); return free;
    }
}
