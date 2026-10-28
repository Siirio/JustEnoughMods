package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SophisticatedFilterProof;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper", remap = false)
public abstract class SophisticatedRestockFilterQuestMixin {
    @Redirect(method = "onHandlerInteract", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedbackpacks/upgrades/restock/RestockUpgradeWrapper;restockFromHandler(Lnet/minecraftforge/items/IItemHandler;)Ljava/util/List;"))
    private List<ItemStack> jem$restock(RestockUpgradeWrapper wrapper, IItemHandler external, IItemHandler original, Player actor) {
        return SophisticatedFilterProof.transfer(wrapper, external, actor, false, () -> wrapper.restockFromHandler(external));
    }
}
