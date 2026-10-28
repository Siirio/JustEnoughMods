package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.inventory.SmithingMenu; import net.minecraft.world.item.crafting.SmithingRecipe; import net.minecraft.world.item.ItemStack; import net.minecraft.world.entity.player.Player;
@Mixin(net.minecraft.world.inventory.SmithingMenu.class)
public abstract class BattleQuestSmithingMixin extends net.minecraft.world.inventory.ItemCombinerMenu {
    protected BattleQuestSmithingMixin(net.minecraft.world.inventory.MenuType<?> type,int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.inventory.ContainerLevelAccess access){super(type,id,inventory,access);}
    @Shadow private SmithingRecipe selectedRecipe;
    @Inject(method="createResult", at=@At("HEAD"))
    private void jem$beforeResult(CallbackInfo ci) { BattleQuestSmithing.beforeResult((SmithingMenu)(Object)this); }
    @Inject(method="createResult", at=@At("RETURN"))
    private void jem$result(CallbackInfo ci) { BattleQuestSmithing.result((SmithingMenu)(Object)this, player); }
    @Inject(method="onTake", at=@At("HEAD"))
    private void jem$begin(Player actor, ItemStack result, CallbackInfo ci) { BattleQuestSmithing.begin((SmithingMenu)(Object)this, selectedRecipe, actor, result); }
    @Inject(method="onTake", at=@At("RETURN"))
    private void jem$end(Player actor, ItemStack result, CallbackInfo ci) { BattleQuestSmithing.end((SmithingMenu)(Object)this, actor); }
}
