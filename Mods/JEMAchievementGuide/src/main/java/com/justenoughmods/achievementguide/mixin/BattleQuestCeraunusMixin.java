package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level; import net.minecraft.world.entity.LivingEntity;
@Mixin(value = com.github.L_Ender.cataclysm.items.Ceraunus.class, remap = false)
public abstract class BattleQuestCeraunusMixin {
    @Inject(method="m_5551_", at=@At("HEAD"))
    private void jem$begin(ItemStack stack, Level level, LivingEntity actor, int remaining, CallbackInfo ci) { BattleQuestArtifacts.begin(actor, stack, "stances"); }
    @Inject(method="m_5551_", at=@At("RETURN"))
    private void jem$end(ItemStack stack, Level level, LivingEntity actor, int remaining, CallbackInfo ci) { BattleQuestArtifacts.end(); }
}
