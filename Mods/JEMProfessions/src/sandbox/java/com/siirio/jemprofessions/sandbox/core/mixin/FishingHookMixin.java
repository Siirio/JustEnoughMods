package com.siirio.jemprofessions.sandbox.core.mixin;

import com.siirio.jemprofessions.sandbox.core.CoreFishing;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Redirect(method = "retrieve", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<ItemStack> jem$actualCatch(List<ItemStack> loot) { return CoreFishing.completed((FishingHook) (Object) this, loot).iterator(); }
}
