package com.siirio.jemfood.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SuspiciousStewItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.function.Consumer;

/** Reuse the native Forge-aware NBT decoder for the actual stack. */
@Mixin(SuspiciousStewItem.class)
public interface SuspiciousStewAccessor {
    @Invoker("listPotionEffects")
    static void jemfood$listEffects(ItemStack stack, Consumer<MobEffectInstance> consumer) {
        throw new IllegalStateException("Suspicious stew accessor was not transformed");
    }
}
