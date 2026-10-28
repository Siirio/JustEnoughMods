package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack;
@Mixin(value = com.github.L_Ender.cataclysm.items.Ignitium_Armor.class, remap = false)
public abstract class BattleQuestIgnitiumArmorMixin {
    @Inject(method="onKeyPacket", at=@At("HEAD"))
    private void jem$begin(Player actor, ItemStack stack, int type, CallbackInfo ci) { BattleQuestArtifacts.begin(actor, stack, "ignitium"); }
    @Inject(method="onKeyPacket", at=@At("RETURN"))
    private void jem$end(Player actor, ItemStack stack, int type, CallbackInfo ci) { BattleQuestArtifacts.end(); }
}
