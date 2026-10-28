package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="net.mcreator.unearthedjourney.procedures.BambooBlowgunRightclickedProcedure",remap=false)
public abstract class WorldPrehistoricQuestBlowgunMixin {
 @Inject(method="execute",at=@At("HEAD"))private static void jem$before(LevelAccessor world,Entity actor,ItemStack stack,CallbackInfo ci){WorldPrehistoricQuestDart.beforeShot(actor,stack);}
 @Redirect(method="execute",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private static boolean jem$shot(Level level,Entity entity){boolean accepted=level.addFreshEntity(entity);if(accepted)WorldPrehistoricQuestDart.accepted(entity);return accepted;}
 @Inject(method="execute",at=@At("RETURN"))private static void jem$after(LevelAccessor world,Entity actor,ItemStack stack,CallbackInfo ci){WorldPrehistoricQuestDart.afterShot();}
}
