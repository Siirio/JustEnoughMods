package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractContainerMenu.class)
public abstract class PetUtilityQuestTradeMenuMixin {
 @Inject(method="clicked",at=@At("HEAD"))private void jem$before(int slot,int button,ClickType type,Player p,CallbackInfo ci){PetUtilityQuestRibbitTrade.begin((AbstractContainerMenu)(Object)this,p);}
 @Inject(method="clicked",at=@At("RETURN"))private void jem$after(int slot,int button,ClickType type,Player p,CallbackInfo ci){PetUtilityQuestRibbitTrade.finish((AbstractContainerMenu)(Object)this,p);}
}
