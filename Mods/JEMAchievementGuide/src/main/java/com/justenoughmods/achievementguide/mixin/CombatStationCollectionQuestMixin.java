package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatStationProof;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class CombatStationCollectionQuestMixin {
    @Unique private List<CombatStationProof.Collection> jem$combatOutput = List.of();
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$begin(int slot, int button, ClickType type, Player actor, CallbackInfo callback) {
        jem$combatOutput = actor instanceof ServerPlayer player ? CombatStationProof.capture((AbstractContainerMenu)(Object)this, player) : List.of();
    }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void jem$end(int slot, int button, ClickType type, Player actor, CallbackInfo callback) {
        if (actor instanceof ServerPlayer player) CombatStationProof.collected((AbstractContainerMenu)(Object)this, player, jem$combatOutput); jem$combatOutput = List.of();
    }
}
