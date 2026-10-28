package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public abstract class CompanionQuestMenuMixin {
 @Shadow private void doClick(int slot,int button,net.minecraft.world.inventory.ClickType type,Player p){throw new AssertionError();}
 @Redirect(method="clicked",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))private void jem$nativeClick(net.minecraft.world.inventory.AbstractContainerMenu menu,int slot,int button,net.minecraft.world.inventory.ClickType type,Player p){CompanionQuestMenu.observe(menu,p,()->doClick(slot,button,type,p));}
}
