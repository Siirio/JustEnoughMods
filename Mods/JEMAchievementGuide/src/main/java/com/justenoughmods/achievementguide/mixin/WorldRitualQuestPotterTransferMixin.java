package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.player.Player;import net.minecraft.world.inventory.*;
@Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public abstract class WorldRitualQuestPotterTransferMixin {
    @Inject(method="clicked",at=@At("HEAD"))private void jem$before(int slot,int button,ClickType type,Player actor,CallbackInfo ci){WorldRitualQuestPotter.begin((AbstractContainerMenu)(Object)this,actor);}
    @Inject(method="clicked",at=@At("RETURN"))private void jem$after(int slot,int button,ClickType type,Player actor,CallbackInfo ci){WorldRitualQuestPotter.end((AbstractContainerMenu)(Object)this,actor);}
}
