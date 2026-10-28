package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.TideCatchProof;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.li64.tide.registries.blocks.AbstractLootCrateBlock", remap = false)
public abstract class RemainingTideCrateOpenMixin {
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;m_5893_(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"))
    private OptionalInt jem$opened(Player player, MenuProvider provider) {
        OptionalInt opened = player.openMenu(provider);
        if (opened.isPresent() && player instanceof ServerPlayer actor && provider instanceof BlockEntity crate
                && actor.containerMenu instanceof ChestMenu menu && menu.getContainer() == provider) TideCatchProof.opened(actor, crate);
        return opened;
    }
}
