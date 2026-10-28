package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.NuclearBombBlock.class,remap=false)
public abstract class CaveQuestNuclearIgnitionMixin {
    @Redirect(method="onCaughtFire",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeIgnition(Level world,Entity bomb,BlockState state,Level original,BlockPos pos,Direction direction,LivingEntity user){
        boolean result=world.addFreshEntity(bomb);var actor=CaveQuestProof.actor(user);
        if(result && actor!=null && CaveQuestProof.permitted(actor,pos) && bomb instanceof com.github.alexmodguy.alexscaves.server.entity.item.NuclearBombEntity){bomb.getPersistentData().putUUID("jemCaveIgniterV4",actor.getUUID());bomb.getPersistentData().putLong("jemCaveIgnitionPosV4",pos.asLong());}return result;
    }
}
