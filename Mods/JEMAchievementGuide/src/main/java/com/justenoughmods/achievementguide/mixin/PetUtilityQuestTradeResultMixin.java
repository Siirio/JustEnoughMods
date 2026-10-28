package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MerchantResultSlot.class)
public abstract class PetUtilityQuestTradeResultMixin {
 @Shadow @Final private Merchant merchant;
 @Shadow @Final private MerchantContainer slots;
 @Unique private PetUtilityQuestRibbitTrade.Debit jem$debit;
 @Inject(method="onTake",at=@At("HEAD"))private void jem$before(Player p,ItemStack stack,CallbackInfo ci){jem$debit=PetUtilityQuestRibbitTrade.debit(merchant,slots);}
 @Inject(method="onTake",at=@At("RETURN"))private void jem$after(Player p,ItemStack stack,CallbackInfo ci){var d=jem$debit;jem$debit=null;PetUtilityQuestRibbitTrade.taken(d,slots,stack);}
}
