package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.item.BeholderEyeEntity;
import com.github.alexmodguy.alexscaves.server.block.blockentity.BeholderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BeholderEyeEntity.class,remap=false)
public abstract class CaveQuestBeholderSessionMixin {
    @Inject(method="handleCameraServerSide",at=@At("RETURN"))private void jem$actualSession(Entity user,boolean enabled,CallbackInfo ci){
        var eye=(BeholderEyeEntity)(Object)this;var actor=CaveQuestProof.actor(user);var proof=eye.getPersistentData();
        if(!enabled || actor==null || !eye.hasTakenFullControlOfCamera || !proof.hasUUID("jemCaveGemActorV4") || !proof.getUUID("jemCaveGemActorV4").equals(actor.getUUID()) || eye.level()!=actor.level())return;
        var pos=BlockPos.of(proof.getLong("jemCaveGemBlockV4"));
        if(CaveQuestProof.loaded(actor.serverLevel(),pos) && actor.serverLevel().getBlockEntity(pos) instanceof BeholderBlockEntity owner && owner.getUsingEntity()==eye){CaveQuestProof.emit(actor,"v3_cave_occult_links_beholder");proof.remove("jemCaveGemActorV4");}
    }
}
