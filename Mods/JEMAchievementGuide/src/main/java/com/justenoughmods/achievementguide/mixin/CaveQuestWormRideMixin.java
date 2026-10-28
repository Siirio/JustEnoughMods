package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.GumWormEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.CandyCaneHookEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=GumWormEntity.class,remap=false)
public abstract class CaveQuestWormRideMixin {
    @Redirect(method="m_7023_",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/entity/living/GumWormEntity;m_6478_(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void jem$actualControlledTravel(GumWormEntity worm,MoverType type,Vec3 delta){
        var before=worm.position();worm.move(type,delta);var actor=CaveQuestProof.actor(worm.getRidingPlayer());var proof=worm.getPersistentData();
        if(actor==null || !worm.isRidingMode() || !worm.isValidRider() || !proof.getBoolean("jemCaveWormPathV4") || !proof.hasUUID("jemCaveWormLureV4")
                || !proof.getUUID("jemCaveWormLureV4").equals(actor.getUUID()) || worm.position().distanceToSqr(before)<=1.0e-8)return;
        if(worm.getHook(true) instanceof CandyCaneHookEntity left && worm.getHook(false) instanceof CandyCaneHookEntity right && left!=right
                && left.getOwner()==actor && right.getOwner()==actor && left.getHookedEntity()==worm && right.getHookedEntity()==worm && !left.isReeling() && !right.isReeling())CaveQuestProof.emit(actor,"v3_cave_worm_rider");
    }
}
