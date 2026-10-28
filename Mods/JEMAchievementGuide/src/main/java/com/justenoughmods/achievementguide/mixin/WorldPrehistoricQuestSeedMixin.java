package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.mcreator.unearthedjourney.UnearthedJourneyMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="net.mcreator.unearthedjourney.procedures.PlatypodiumSeedPodsRightclickedOnBlockProcedure",remap=false)
public abstract class WorldPrehistoricQuestSeedMixin {
 @Inject(method="execute",at=@At("HEAD"))private static void jem$before(LevelAccessor world,double x,double y,double z,Entity e,ItemStack stack,CallbackInfo ci){WorldPrehistoricQuestAsync.beginSeed(e,stack);}
 @Inject(method="execute",at=@At("RETURN"))private static void jem$after(LevelAccessor world,double x,double y,double z,Entity e,ItemStack stack,CallbackInfo ci){WorldPrehistoricQuestAsync.finishSeed(stack);}
 @Redirect(method="execute",at=@At(value="INVOKE",target="Lnet/mcreator/unearthedjourney/UnearthedJourneyMod;queueServerWork(ILjava/lang/Runnable;)V"))private static void jem$nativeQueue(int delay,Runnable work){UnearthedJourneyMod.queueServerWork(delay,WorldPrehistoricQuestAsync.nativeWork(work));}
 @Redirect(method={"lambda$execute$0","lambda$execute$1"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;m_230328_(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/util/RandomSource;I)Z"))private static boolean jem$nativeGrowth(StructureTemplate template,ServerLevelAccessor level,BlockPos origin,BlockPos pivot,StructurePlaceSettings settings,RandomSource random,int flags){WorldPrehistoricQuestAsync.beginGrowth();boolean result=false;try{result=template.placeInWorld(level,origin,pivot,settings,random,flags);return result;}finally{WorldPrehistoricQuestAsync.finishGrowth(result);}}
}
