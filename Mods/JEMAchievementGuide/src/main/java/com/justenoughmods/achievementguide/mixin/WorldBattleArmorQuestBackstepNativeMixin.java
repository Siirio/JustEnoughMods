package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.github.L_Ender.cataclysm.items.Cursium_Armor",remap=false) public abstract class WorldBattleArmorQuestBackstepNativeMixin {
 @Unique private static final ThreadLocal<java.util.ArrayDeque<java.util.Optional<WorldBattleArmorQuestMotion.Launch>>> JEM_LAUNCH=ThreadLocal.withInitial(java.util.ArrayDeque::new);
 @Inject(method="onKeyPacket",at=@At("HEAD"),remap=false) private void jem$before(Player player,ItemStack stack,int type,CallbackInfo ci){var actor=WorldBattleArmorQuestProof.real(player);JEM_LAUNCH.get().push(java.util.Optional.ofNullable(actor==null?null:WorldBattleArmorQuestMotion.launchBefore(actor,stack,type)));}
 @Inject(method="onKeyPacket",at=@At("RETURN"),remap=false) private void jem$after(Player player,ItemStack stack,int type,CallbackInfo ci){var calls=JEM_LAUNCH.get();var launch=calls.isEmpty()?null:calls.pop().orElse(null);if(calls.isEmpty())JEM_LAUNCH.remove();WorldBattleArmorQuestMotion.launchAfter(launch,stack);}
}
