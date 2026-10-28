package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.blockentity.ConfectionOvenBlockEntity.class,remap=false)
public abstract class CaveQuestOvenMixin {
    @Redirect(method="makeGingerbreadMen",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeHelper(Level level,Entity helper){boolean result=level.addFreshEntity(helper);var actor=CaveQuestProof.owner((BlockEntity)(Object)this);
        if(result && actor!=null && helper instanceof com.github.alexmodguy.alexscaves.server.entity.living.GingerbreadManEntity ginger && ginger.isOvenSpawned())helper.getPersistentData().putUUID("jemCaveOvenActorV4",actor.getUUID());return result;}
}
