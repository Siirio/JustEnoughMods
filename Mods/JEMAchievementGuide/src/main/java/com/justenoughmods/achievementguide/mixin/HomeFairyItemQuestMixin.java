package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeFairyProof;
import me.paulf.fairylights.server.fastener.Fastener;
import me.paulf.fairylights.server.item.ConnectionItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ConnectionItem.class, remap = false)
public abstract class HomeFairyItemQuestMixin {
    @Inject(method = "connect(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lme/paulf/fairylights/server/fastener/Fastener;Z)V", at = @At("HEAD"))
    private void jem$before(ItemStack stack, Player player, Level level, Fastener<?> fastener, boolean sound, CallbackInfo callback) {
        HomeFairyProof.begin(player);
    }
    @Inject(method = "connect(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lme/paulf/fairylights/server/fastener/Fastener;Z)V", at = @At("RETURN"))
    private void jem$after(ItemStack stack, Player player, Level level, Fastener<?> fastener, boolean sound, CallbackInfo callback) {
        HomeFairyProof.end();
    }
}
