package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeRegisterAccess;
import com.justenoughmods.achievementguide.integration.home.HomeRegisterProof;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class HomeRegisterClickQuestMixin {
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$before(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        if ((Object)this instanceof HomeRegisterAccess) HomeRegisterProof.begin((AbstractContainerMenu)(Object)this, player);
    }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void jem$after(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        if ((Object)this instanceof HomeRegisterAccess) HomeRegisterProof.finish((AbstractContainerMenu)(Object)this, player);
    }
}
