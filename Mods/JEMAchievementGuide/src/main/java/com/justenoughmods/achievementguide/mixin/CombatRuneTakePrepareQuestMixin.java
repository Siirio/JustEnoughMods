package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneAnvilProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class CombatRuneTakePrepareQuestMixin {
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$prepare(int slot, int button, ClickType kind, Player player, CallbackInfo callback) { if ((Object)this instanceof AnvilMenu menu && slot == 2 && player instanceof ServerPlayer actor) CombatRuneAnvilProof.prepare(menu, actor); }
}
