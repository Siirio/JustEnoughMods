package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = {"net.mcreator.skarriermobs.item.HeartOfTranquilityItem", "net.mcreator.skarriermobs.item.LeechingSackItem"}, remap = false)
public abstract class SkarrierConsumableQuestMixin {
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$begin(ItemStack item, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        SkarrierWorkshopProof.beginUse(actor, item);
    }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$end(ItemStack item, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) { SkarrierWorkshopProof.end(); }
}
