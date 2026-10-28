package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.item.AbstractMovingBlockEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.FallingTreeBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=AbstractMovingBlockEntity.class,remap=false)
public abstract class CaveQuestTreeDropMixin {
    @Redirect(method="createBlockDropAt",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;m_49840_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void jem$nativeTreeDrop(Level level,BlockPos pos,ItemStack item){
        var tree=(AbstractMovingBlockEntity)(Object)this;var actor=tree instanceof FallingTreeBlockEntity?CaveQuestProof.owner(tree,"jemCaveFellingActorV4"):null;
        CaveQuestDrops.observe(tree,actor,"forestry",()->{Block.popResource(level,pos,item);return true;});
    }
}
