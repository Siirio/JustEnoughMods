package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BackpackCookingProof;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class BackpackCookingMenuQuestMixin {
    @Unique private List<BackpackCookingProof.Collection> jem$cookedCollections = List.of();
    @Inject(method = "m_150399_", at = @At("HEAD"))
    private void jem$beforeCollection(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        jem$cookedCollections = (Object) this instanceof BackpackContainer menu && player instanceof ServerPlayer actor
                && actor.containerMenu == menu ? BackpackCookingProof.capture(menu, actor) : List.of();
    }
    @Inject(method = "m_150399_", at = @At("RETURN"))
    private void jem$afterCollection(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        var collections = jem$cookedCollections;
        jem$cookedCollections = List.of();
        if ((Object) this instanceof BackpackContainer menu && player instanceof ServerPlayer actor) BackpackCookingProof.collected(menu, actor, collections);
    }
}
