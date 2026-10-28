package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneAnvilProof;
import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AnvilMenu.class)
public abstract class CombatRuneAnvilQuestMixin {
    @Unique private CombatRuneAnvilProof.Take jem$runeTake;
    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$begin(Player player, ItemStack taken, CallbackInfo callback) { if (player instanceof ServerPlayer actor) { AnvilMenu menu = (AnvilMenu)(Object)this; ItemStack result = taken.isEmpty() ? ((QuestResultSnapshot)menu).jem$questQuickResult() : taken; jem$runeTake = CombatRuneAnvilProof.begin(menu, actor, result); } }
    @Inject(method = "onTake", at = @At("RETURN"))
    private void jem$end(Player player, ItemStack taken, CallbackInfo callback) { CombatRuneAnvilProof.Take proof = jem$runeTake; jem$runeTake = null; if (player instanceof ServerPlayer actor) CombatRuneAnvilProof.end((AnvilMenu)(Object)this, actor, taken, proof); }
}
