package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastResourceProof;
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
public abstract class MagicLastResourceTakeQuestMixin {
    @Unique private List<MagicLastResourceProof.Take> jem$arcaneTake = List.of();
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$before(int slot, int button, ClickType type, Player user, CallbackInfo callback) { jem$arcaneTake = user instanceof ServerPlayer actor ? MagicLastResourceProof.capture((AbstractContainerMenu)(Object)this, actor) : List.of(); }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void jem$after(int slot, int button, ClickType type, Player user, CallbackInfo callback) { if (user instanceof ServerPlayer actor) MagicLastResourceProof.collected((AbstractContainerMenu)(Object)this, actor, jem$arcaneTake); jem$arcaneTake = List.of(); }
}
