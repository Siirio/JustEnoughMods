package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.item.Item;
@Mixin(net.minecraft.world.item.ItemCooldowns.class)
public abstract class BattleQuestCooldownMixin {
    @Inject(method="addCooldown", at=@At("RETURN"))
    private void jem$cooldown(Item item, int duration, CallbackInfo ci) { BattleQuestArtifacts.cooldown((net.minecraft.world.item.ItemCooldowns)(Object)this, item, duration); }
}
