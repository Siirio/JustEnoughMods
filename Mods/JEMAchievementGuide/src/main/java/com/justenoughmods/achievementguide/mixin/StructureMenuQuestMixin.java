package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.StructureLootProof;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class StructureMenuQuestMixin {
    @Unique private AbstractContainerMenu jem$lootMenu;
    @Unique private Set<String> jem$lootActions = Set.of();

    @Inject(method = "openMenu", at = @At("HEAD"))
    private void jem$beginOpen(MenuProvider provider, CallbackInfoReturnable<OptionalInt> callback) {
        jem$lootMenu = null;
        jem$lootActions = Set.of();
    }

    @Redirect(method = "openMenu", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"))
    private AbstractContainerMenu jem$nativeMenu(MenuProvider provider, int id, Inventory inventory, Player player) {
        StructureLootProof.Opened opened = StructureLootProof.create(provider, id, inventory, player);
        jem$lootMenu = opened.menu();
        jem$lootActions = opened.actions();
        return opened.menu();
    }

    @Inject(method = "openMenu", at = @At("RETURN"))
    private void jem$opened(MenuProvider provider, CallbackInfoReturnable<OptionalInt> callback) {
        ServerPlayer actor = (ServerPlayer) (Object) this;
        var actions = jem$lootActions;
        AbstractContainerMenu menu = jem$lootMenu;
        jem$lootActions = Set.of();
        jem$lootMenu = null;
        if (callback.getReturnValue().isPresent() && menu != null && actor.containerMenu == menu) {
            for (String action : actions) QuestActionTrigger.INSTANCE.emit(actor, action);
        }
    }
}
