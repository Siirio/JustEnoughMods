package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SophisticatedFilterProof;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper", remap = false)
public abstract class SophisticatedFilterTransferQuestMixin {
    @Redirect(method = "onHandlerInteract", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedbackpacks/upgrades/deposit/DepositUpgradeWrapper;depositToHandler(Lnet/minecraftforge/items/IItemHandler;)Ljava/util/List;"))
    private List<ItemStack> jem$deposit(DepositUpgradeWrapper wrapper, IItemHandler external, IItemHandler original, Player actor) {
        return SophisticatedFilterProof.transfer(wrapper, external, actor, true, () -> wrapper.depositToHandler(external));
    }
}
