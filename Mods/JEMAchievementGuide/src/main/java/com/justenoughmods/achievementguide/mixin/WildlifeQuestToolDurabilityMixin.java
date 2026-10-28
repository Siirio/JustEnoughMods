package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestSources;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestCrafts;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class WildlifeQuestToolDurabilityMixin {
    @Unique private final ThreadLocal<ItemStack> jem$durability = new ThreadLocal<>();
    @Inject(method = "hurtAndBreak", at = @At("HEAD"))
    private void jem$before(int amount, LivingEntity entity, Consumer<LivingEntity> broken, CallbackInfo callback) {
        jem$durability.remove();
        if (entity.level() instanceof ServerLevel && WildlifeQuestCrafts.TOOLS.containsKey(WildlifeQuestProof.item((ItemStack)(Object)this))) jem$durability.set(((ItemStack)(Object)this).copy());
    }
    @Inject(method = "hurtAndBreak", at = @At("RETURN"))
    private void jem$after(int amount, LivingEntity entity, Consumer<LivingEntity> broken, CallbackInfo callback) {
        ItemStack before = jem$durability.get(); jem$durability.remove();
        if (before != null && entity.level() instanceof ServerLevel level) WildlifeQuestSources.durability(level, before, (ItemStack)(Object)this);
    }
}
