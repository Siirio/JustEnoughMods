package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestMenuDebit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(net.minecraft.server.network.ServerGamePacketListenerImpl.class)
public abstract class CaveQuestMenuMixin {
    @Redirect(method="handleContainerClick",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))
    private void jem$actualNativeClick(AbstractContainerMenu menu,int slot,int button,ClickType type,Player user){
        if(user instanceof ServerPlayer actor)CaveQuestMenuDebit.observe(menu,actor,()->menu.clicked(slot,button,type,user));else menu.clicked(slot,button,type,user);
    }
}
