package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.GumWormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.block.blockentity.GobthumperBlockEntity.class,remap=false)
public abstract class CaveQuestGobthumperMixin {
    @Redirect(method="thumpTick",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/entity/living/GumWormEntity;setGobthumperPos(Lnet/minecraft/core/BlockPos;)V"))
    private void jem$actualAttraction(GumWormEntity worm,BlockPos pos){
        var before=worm.getGobthumperPos();worm.setGobthumperPos(pos);var actor=CaveQuestProof.owner((BlockEntity)(Object)this);
        if(actor!=null && pos.equals(worm.getGobthumperPos()) && !pos.equals(before)){
            worm.getPersistentData().putUUID("jemCaveWormLureV4",actor.getUUID());worm.getPersistentData().putLong("jemCaveWormLurePosV4",pos.asLong());worm.getPersistentData().remove("jemCaveWormPathV4");
        }
    }
}
