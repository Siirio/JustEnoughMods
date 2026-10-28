package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraft.world.inventory.*;import net.minecraft.world.entity.player.Player;import java.util.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class) public abstract class WorldBattleArmorQuestFusionClickMixin {
 @Unique private final ArrayDeque<Optional<WorldBattleArmorQuestFusion.Click>> jem$armorClicks=new ArrayDeque<>();
 @Inject(method="clicked",at=@At("HEAD")) private void jem$begin(int slot,int button,ClickType type,Player player,CallbackInfo ci){jem$armorClicks.push(Optional.ofNullable(WorldBattleArmorQuestFusion.begin((AbstractContainerMenu)(Object)this,player)));}
 @Inject(method="clicked",at=@At("RETURN")) private void jem$end(int slot,int button,ClickType type,Player player,CallbackInfo ci){if(!jem$armorClicks.isEmpty())WorldBattleArmorQuestFusion.end(jem$armorClicks.pop().orElse(null));}
}
