package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestCargo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class WildlifeQuestCargoClickMixin {
    @Unique private WildlifeQuestCargo.Click jem$click;
    @Unique private com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBrews.Deposit jem$brewDeposit;
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$before(int slot, int button, ClickType type, Player player, CallbackInfo callback) { jem$click = player instanceof ServerPlayer actor ? WildlifeQuestCargo.begin(actor, (AbstractContainerMenu)(Object)this) : null; jem$brewDeposit = player instanceof ServerPlayer actor ? com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBrews.begin(actor, (AbstractContainerMenu)(Object)this) : null; }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void jem$after(int slot, int button, ClickType type, Player player, CallbackInfo callback) { WildlifeQuestCargo.end(jem$click); jem$click = null; com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBrews.deposited(jem$brewDeposit); jem$brewDeposit = null; }
}
