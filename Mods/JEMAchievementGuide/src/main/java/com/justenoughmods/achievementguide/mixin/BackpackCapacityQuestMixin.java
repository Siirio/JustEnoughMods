package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BackpackCapacityProof;
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
public abstract class BackpackCapacityQuestMixin {
    @Unique private BackpackCapacityProof.Before jem$capacityBefore;
    @Inject(method = "m_150399_", at = @At("HEAD"))
    private void jem$beforeCapacity(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        jem$capacityBefore = (Object) this instanceof BackpackContainer menu && player instanceof ServerPlayer actor
                && actor.containerMenu == menu ? BackpackCapacityProof.before(menu, actor) : null;
    }
    @Inject(method = "m_150399_", at = @At("RETURN"))
    private void jem$afterCapacity(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        var before = jem$capacityBefore;
        jem$capacityBefore = null;
        if ((Object) this instanceof BackpackContainer menu && player instanceof ServerPlayer actor) BackpackCapacityProof.after(menu, actor, before);
    }
}
