package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestNuclearHatch;
import com.github.alexmodguy.alexscaves.server.block.TremorzillaEggBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity.class,remap=false)
public abstract class CaveQuestNuclearHatchMixin {
    @Redirect(method="removeChunk",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/block/TremorzillaEggBlock;spawnDinosaurs(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void jem$actualNuclearEgg(TremorzillaEggBlock egg,Level level,BlockPos pos,BlockState state){CaveQuestNuclearHatch.observe((Entity)(Object)this,pos,()->egg.spawnDinosaurs(level,pos,state));}
}
